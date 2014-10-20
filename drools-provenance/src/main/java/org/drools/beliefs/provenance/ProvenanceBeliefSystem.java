package org.drools.beliefs.provenance;

import org.drools.core.beliefsystem.BeliefSet;
import org.drools.core.beliefsystem.simple.SimpleBeliefSystem;
import org.drools.core.beliefsystem.simple.SimpleLogicalDependency;
import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.LogicalDependency;
import org.drools.core.common.NamedEntryPoint;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.factmodel.traits.BitMaskKey;
import org.drools.core.factmodel.traits.TraitFactory;
import org.drools.core.factmodel.traits.TraitProxy;
import org.drools.core.metadata.Don;
import org.drools.core.metadata.MetaCallableTask;
import org.drools.core.metadata.Modify;
import org.drools.core.metadata.NewInstance;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.reteoo.PropertySpecificUtil;
import org.drools.core.spi.Activation;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.BitMaskUtil;
import org.w3.ns.prov.Activity;

import java.util.Collection;

public class ProvenanceBeliefSystem
        extends SimpleBeliefSystem
        implements Provenance {
    
    protected ProvenanceBeliefSystem( NamedEntryPoint ep, TruthMaintenanceSystem tms ) {
        super( ep, tms );
    }

    @Override
    public void delete( LogicalDependency node, BeliefSet beliefSet, PropagationContext context ) {
        if ( ! ( node.getObject() instanceof MetaCallableTask ) ) {
            super.delete( node, beliefSet, context );
        }
    }

    @Override
    public void insert( LogicalDependency node, BeliefSet beliefSet, PropagationContext context, ObjectTypeConf typeConf ) {

        if ( node.getObject() instanceof MetaCallableTask ) {

            PropagationContext unravelContext = getEp().getInternalWorkingMemory().getKnowledgeBase().getConfiguration().getComponentFactory().getPropagationContextFactory().createPropagationContext(
                    getEp().getInternalWorkingMemory().getNextPropagationIdCounter(),
                    PropagationContext.DELETION,
                    node.getJustifier().getRule(),
                    node.getJustifier().getTuple(),
                    beliefSet.getFactHandle()
            );
            MetaCallableTask task = (MetaCallableTask) node.getObject();
            InternalFactHandle taskHandle = beliefSet.getFactHandle();

            getEp().getObjectStore().removeHandle( taskHandle );
            getEp().delete( taskHandle, task, typeConf, node.getJustifier().getRule(), node.getJustifier() );
            getEp().getTruthMaintenanceSystem().remove( getEp().getTruthMaintenanceSystem().get( task ) );


            switch ( task.kind() ) {
                case ASSERT :
                    beliefSet = ensureBeliefSetConsistency( beliefSet, executeNew( (NewInstance) task, node ) );
                    break;
                case MODIFY : executeModify( (Modify) task, node );
                    beliefSet = ensureBeliefSetConsistency( beliefSet, ( (Modify) task ).getTarget() );
                    break;
                case DON    : executeDon( (Don) task, node );
                    beliefSet = ensureBeliefSetConsistency( beliefSet, ( (Don) task ).getCore() );
                    break;
                default:
                    throw new UnsupportedOperationException( "Unrecognized Meta TASK type" );
            }

            beliefSet.add( node.getMode() );

            if ( task.kind() == MetaCallableTask.KIND.MODIFY && ((Modify) task).getAdditionalUpdates() != null ) {
                for ( Object extra : ( (Modify) task ).getAdditionalUpdates() ) {
                    if ( extra instanceof TraitProxy ) {
                        extra = ((TraitProxy) extra).getObject();
                    }
                    EqualityKey ek = getTms().get( extra );

                    if ( ek != null ) {
                        if ( ek.getBeliefSet() == null ) {
                            ek.setBeliefSet( newBeliefSet( ek.getFactHandle() ) );
                        }
                        ek.getBeliefSet().add( node.getMode() );
                    }
                }
            }

        } else {
            super.insert( node, beliefSet, context, typeConf );
        }


    }

    private BeliefSet ensureBeliefSetConsistency( BeliefSet beliefSet, Object core ) {
        if ( beliefSet.getFactHandle().getObject() != core ) {
            if ( core instanceof TraitProxy ) {
                core = ((TraitProxy) core).getObject();
            }
            EqualityKey ek = getTms().get( core );
            // update the belief set TODO even stated objects should be allowed to have a BS
            if ( ek.getBeliefSet() == null ) {
                BeliefSet bset = newBeliefSet( ek.getFactHandle() );
                ek.setBeliefSet( bset );
                return bset;
            } else {
                return ek.getBeliefSet();
            }
        }
        return beliefSet;
    }

    private void executeDon( Don don, LogicalDependency node ) {
        getEp().getTraitHelper().don( node.getJustifier(),
                                    don.getCore(),
                                    don.getTrait(),
                                    null,
                                    false );
    }

    private void executeModify( Modify modify, LogicalDependency node ) {
        Object target = modify.getTarget();
        modify.call( getEp().getKnowledgeBase() );

        getEp().update( (InternalFactHandle) getEp().getFactHandle( target ),
                   target,
                   modify.getModificationMask(),
                   modify.getModificationClass(),
                   node.getJustifier() );

        Object[] updates = modify.getAdditionalUpdates();
        if ( updates != null ) {
            for ( int j = 0; j < updates.length; j++ ) {
                getEp().update( (InternalFactHandle) getEp().getFactHandle( updates[ j ] ),
                           updates[ j ],
                           modify.getAdditionalUpdatesModificationMask( j ),
                           updates[ j ].getClass(),
                           node.getJustifier() );
            }
        }
    }

    private Object executeNew( NewInstance newInstance, LogicalDependency node ) {
        if ( newInstance.isInterface() ) {
            newInstance.setInstantiatorFactory( TraitFactory.getTraitBuilderForKnowledgeBase( getEp().getKnowledgeBase() ).getInstantiatorFactory() );
            Object target = newInstance.callUntyped();

            getEp().getTraitHelper().don( node.getJustifier(),
                                        target,
                                        newInstance.getInstanceClass(),
                                        newInstance.getInitArgs(),
                                        false );
            return target;
        } else {
            Object target = newInstance.call();
            getEp().insert( target,
                      false,
                       node.getJustifier().getRule(),
                       node.getJustifier() );
            return target;
        }
    }

    public BeliefSet newBeliefSet( InternalFactHandle fh ) {
        return new ProvenanceBeliefSetImpl( this, fh );
    }

    protected ProvenanceBeliefSet getProvenanceBS( Object o ) {
        EqualityKey key = getTruthMaintenanceSystem().get( o );
        if ( key == null || ! ( key.getBeliefSet() instanceof ProvenanceBeliefSetImpl ) ) {
            return null;
        }
        return (ProvenanceBeliefSet) key.getBeliefSet();
    }

    @Override
    public boolean hasProvenanceFor( Object o ) {
        return getProvenanceBS( o ) != null;
    }

    @Override
    public Collection<? extends Activity> describeProvenance( Object o ) {
        return getProvenanceBS( o ).getGeneratingActivities();
    }
}