import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.drools.beliefs.provenance.Provenance;
import org.drools.beliefs.provenance.ProvenanceBeliefSystem;
import org.drools.beliefs.provenance.ProvenanceHelper;
import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.NamedEntryPoint;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.marshalling.impl.ProtobufMarshaller;
import org.drools.core.metadata.Identifiable;
import org.drools.core.metadata.Modify;
import org.drools.core.rule.EntryPointId;
import org.drools.core.util.Entry;
import org.drools.core.util.ObjectHashMap;
import org.jboss.drools.provenance.Assertion;
import org.jboss.drools.provenance.Modification;
import org.jboss.drools.provenance.Property;
import org.jboss.drools.provenance.Recognition;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.SessionClock;
import org.kie.internal.marshalling.MarshallerFactory;
import org.kie.internal.utils.KieHelper;
import org.test.MyKlass;
import org.test.MyKlassImpl;
import org.w3.ns.prov.Activity;
import org.w3.ns.prov.Entity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProvenanceTest {

    @Test
    public void testProvenanceWithStatedObjects() {
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks_simpleSet.drl", list );

        MyKlass mk = (MyKlass) new MyKlassImpl().withDyEntryId( "123-000" );
        kieSession.insert( mk );
        kieSession.fireAllRules();

        TruthMaintenanceSystem tms = ( (NamedEntryPoint) kieSession.getEntryPoint( EntryPointId.DEFAULT.getEntryPointId() ) ).getTruthMaintenanceSystem();
        assertEquals( 1, tms.getEqualityKeyMap().size() );
        EqualityKey ek = (EqualityKey) ((ObjectHashMap.ObjectEntry) tms.getEqualityKeyMap().iterator().next()).getValue();

        Object core = ek.getFactHandle().getObject();
        assertSame( mk, core );
    }

    @Test
    public void testMetaCallableWMTasksSetSimpleAttribute() {
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks_simpleSet.drl", list );
        InternalFactHandle handle = (InternalFactHandle) kieSession.insert( new MyKlassImpl().withDyEntryId( "123-000" ) );
        kieSession.fireAllRules();

        System.out.println( list );
        assertEquals( Arrays.asList( "fooVal" ), list );

        List<Activity> history = getProvenanceHistory( kieSession, handle.getObject() );
        assertEquals( 1, history.size() );

        Activity act = history.iterator().next();

        assertTrue( act instanceof Modification );
        assertEquals( 1, act.getGenerated().size() );
        assertTrue( act.getGenerated().iterator().next() instanceof Property );

        Property prop = (Property) act.getGenerated().iterator().next();
        assertEquals( "fooVal", prop.getValue().iterator().next().getLit() );
        assertEquals( ( (Identifiable) handle.getObject() ).getId().toString(),
                      prop.getHadPrimarySource().iterator().next().getIdentifier().iterator().next().toString() );
        assertEquals( "http://www.test.org#prop", prop.getIdentifier().iterator().next().toString() );

    }

    @Test
    public void testMetaCallableWMTasksSetSimpleAttributeMultipleVersionedTimes() {
        // set a simple attribute multiple times, keep history
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks_simpleHistory.drl", list );
        InternalFactHandle handle = (InternalFactHandle) kieSession.insert( new MyKlassImpl().withDyEntryId( "123-000" ) );
        kieSession.fireAllRules();

        System.out.println( list );
        // null is in the list since a high priority rule checks the fact even before the properties are set
        assertEquals( Arrays.asList( null, "fooVal", "barVal" ), list );

        List<Activity> history = getProvenanceHistory( kieSession, handle.getObject() );
        assertEquals( 1, history.size() );

        Activity act = history.get( 0 );
        Property prop = (Property) act.getGenerated().iterator().next();
        assertEquals( "barVal", prop.getValue().iterator().next().getLit() );
        assertEquals( ( (Identifiable) handle.getObject() ).getId().toString(),
                      prop.getHadPrimarySource().iterator().next().getIdentifier().iterator().next().toString() );
        assertEquals( "http://www.test.org#prop", prop.getIdentifier().iterator().next().toString() );

        assertEquals( 1, act.getWasInformedBy().size() );
        Activity prev = act.getWasInformedBy().get( 0 );
        Property prevProp = (Property) prev.getGenerated().iterator().next();
        assertEquals( "fooVal", prevProp.getValue().iterator().next().getLit() );
    }

    @Test
    @Ignore
    public void testMetaCallableWMTasksSetCollectionAsAWhole() {
        // set a list attribute
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );
        fail( "TODO" );
    }

    @Test
    @Ignore
    public void testMetaCallableWMTasksAddItemToCollection() {
        // add an item to a list, versioning it
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );
        fail( "TODO" );
    }

    @Test
    @Ignore
    public void testMetaCallableWMTasksAddItemsToCollection() {
        // add a collection to a list, versioning it
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );
        fail( "TODO" );
    }

    @Test
    @Ignore
    public void testMetaCallableWMTasksRemoveItemFromCollection() {
        // remove an item from a list, versioning it
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );
        fail( "TODO" );
    }

    @Test
    @Ignore
    public void testMetaCallableWMTasksRemoveItemsFromCollection() {
        // remove a collection from a list, versioning it
        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );
        fail( "TODO" );
    }




    @Test
    public void testPersistence() throws IOException {
        List list = new ArrayList();
        KieSession ksession = loadProvenanceSession( "testTasks.drl", list );
        ksession.fireAllRules();

        ProtobufMarshaller marshaller = (ProtobufMarshaller) MarshallerFactory.newMarshaller( ksession.getKieBase(),
                                                                                              (ObjectMarshallingStrategy[]) ksession.getEnvironment().get( EnvironmentName.OBJECT_MARSHALLING_STRATEGIES ) );
        long time = ksession.<SessionClock>getSessionClock().getCurrentTime();

        // Serialize object
        final byte [] b1;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshall( bos,
                             ksession,
                             time );

        b1 = bos.toByteArray();
        bos.close();
    }


    @Test
    public void testMetaCallableWMTasks() {

        List list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testTasks.drl", list );

        for ( Object o : kieSession.getObjects() ) {
            System.out.println( o );
        }
        System.out.println( list );
        assertEquals( Arrays.asList( "123", "000" ), list );

        TruthMaintenanceSystem tms = ( (NamedEntryPoint) kieSession.getEntryPoint( EntryPointId.DEFAULT.getEntryPointId() ) ).getTruthMaintenanceSystem();
        assertEquals( 2, tms.getEqualityKeyMap().size() );

        for ( Object o : kieSession.getObjects() ) {
            if ( o instanceof Entity ) {
                Collection<? extends Activity> acts = ProvenanceHelper.getProvenance( kieSession ).describeProvenance( o );
                // creation, typing and setting of property "links"
                assertEquals( 3, acts.size() );
            }
        }
    }

    @Test
    @Ignore
    public void testConceptualModelBindingWithTraitDRL() {

        ArrayList list = new ArrayList();
        KieSession kieSession = loadProvenanceSession( "testProvenance.drl", list );

        Provenance provenance = ProvenanceHelper.getProvenance( kieSession );
        assertEquals( Arrays.asList( "RESULT" ), list );

        for ( Object o : kieSession.getObjects() ) {
            if ( provenance.hasProvenanceFor( o ) ) {
                Collection<? extends Activity> acts = provenance.describeProvenance( o );

                assertEquals( 4, acts.size() );

                for ( Activity act : acts ) {
                    if ( act instanceof Recognition ) {
                        assertEquals( 1, act.getGenerated().size() );
                        Entity rec = act.getGenerated().get( 0 );
                        Entity tgt = rec.getHadPrimarySource().get( 0 );

                        assertEquals( 1, tgt.getDisplaysAs().size() );
                        assertEquals( "Pretty print hello world from IdentifiableEntity_Proxy",
                                      tgt.getDisplaysAs().get( 0 ).getNarrativeText().get( 0 ) );
                    }
                }
            }
        }


    }


    private KieSession loadProvenanceSession( String sourceDrl, List list) {
        KieServices kieServices = KieServices.Factory.get();
        Resource traitDRL = kieServices.getResources().newClassPathResource( "tiny_declare.drl" );
        Resource ruleDRL = kieServices.getResources().newClassPathResource( sourceDrl );

        KieHelper kieHelper = validateKieBuilder( traitDRL, ruleDRL );
        KieSession kieSession = kieHelper.build( ProvenanceHelper.getProvenanceEnabledKieBaseConfiguration() ).newKieSession();
        kieSession.setGlobal( "list", list );
        kieSession.fireAllRules();

        return kieSession;
    }


    private KieHelper validateKieBuilder( Resource... resources ) {
        KieHelper helper = new KieHelper();
        for ( Resource resource : resources ) {
            helper.addResource( resource );
        }

        Results res = helper.verify();
        System.err.println( res.getMessages() );
        assertFalse( helper.verify().hasMessages( Message.Level.ERROR ) );

        return helper;
    }


    public List<Activity> getProvenanceHistory( KieSession kieSession, Object o ) {
        Provenance provenance = ProvenanceHelper.getProvenance( kieSession );
        return new ArrayList( provenance.describeProvenance( o ) );
    }
}