import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.EmpireOptions;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.empire.sesametwo.RepositoryDataSourceFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.owl.conyard.Equipment;
import org.drools.owl.conyard.EquipmentImpl;
import org.drools.owl.conyard.Guest;
import org.drools.owl.conyard.GuestImpl;
import org.drools.owl.conyard.IronInstallation;
import org.drools.owl.conyard.IronInstallationImpl;
import org.drools.owl.conyard.Labourer;
import org.drools.owl.conyard.LabourerImpl;
import org.drools.owl.conyard.ObjectFactory;
import org.drools.owl.conyard.Paint;
import org.drools.owl.conyard.PaintImpl;
import org.drools.owl.conyard.Painting;
import org.drools.owl.conyard.PaintingImpl;
import org.drools.owl.conyard.Person;
import org.drools.owl.conyard.PersonImpl;
import org.drools.owl.conyard.Site;
import org.drools.owl.conyard.SiteImpl;
import org.drools.owl.conyard.Smith;
import org.drools.owl.conyard.SmithImpl;
import org.drools.owl.conyard.Stair;
import org.drools.owl.conyard.StairImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceProvider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FactTest {








    private static ObjectFactory factory = new ObjectFactory();

    private static Painting painting;
    private static Person pers;
    private static Site site;




    @BeforeClass
    public static void init() {

    }





    @Before
    public void initTestData() {

        painting = new PaintingImpl();


        painting.setStartsOn(new Date());
        painting.setEndsOn(new Date());
        painting.setHasComment("Some comment on this object");

        Stair stair = new StairImpl();

        stair.setStairLength(10);
        stair.setOid( "stairId" );

        painting.setRequiresStair( stair );

        painting.setOid("oidX");

        painting.addRequiresAlso( stair );

        Person px = new PersonImpl();
            px.setOid( "aGuy" );
        painting.addInvolves( px );

        Labourer lab = new LabourerImpl();
            lab.setOid( "123456x" );
        painting.addInvolves( lab  );

        Paint paint = new PaintImpl();
        paint.setOid( "R0_G0_B0" );


        site = new SiteImpl();
        site.setOid( "siteId" );

        site.setCenterX(10.0f);
        site.setCenterY(20.0f);
        site.setRadius(100.0f);
        paint.setStoredInSite( site );
        painting.addRequires( paint );

        Equipment equip = new EquipmentImpl();
            equip.setOid( "ekwip1" );
        painting.addRequires( equip );


        pers = new LabourerImpl();
        pers.setOid( "pers2" );


        pers.addParticipatesIn( painting );
        painting.addInvolves( pers );


    }




    public void checkPainting( Painting painting ) {
        assertNotNull( painting );

        assertEquals( 3, painting.getRequires().size() );

        assertEquals( 1, painting.getRequiresAlso().size() );
        assertTrue( painting.getRequiresAlso().get( 0 ) instanceof StairImpl );
        assertEquals( 10, (int) ((Stair) painting.getRequiresAlso().get( 0 )).getStairLength() );

        assertEquals( 3, painting.getInvolves().size() );

//        boolean found = false;
//        for ( Person p : painting.getInvolves() ) {
//            if ( p.getParticipatesIn().size() > 0 ) {
//                assertEquals( painting.getOid(), p.getParticipatesIn().get( 0 ).getOid() );
//                found = true;
//            }
//        }
//        assertTrue( found );

    }

    @Test
    public void testFact() {
        checkPainting( painting );

        assertEquals( 3, pers.getMakesUseOf().size() );
    }




    @Test
    public void testJaxb() throws JAXBException {

        Painting p2 = (Painting) refreshOnJaxb( painting );

        assertEquals( painting, p2 );
        assertEquals( p2, painting );
        checkPainting( p2 );

    }


    @Test
    public void testSemanticAccessors() {
        Painting pain = new PaintingImpl();

        Date d = new Date();
        pain.setStartsOn(d);
        assertEquals( d, pain.getStartsOn() );

        Labourer lab = new LabourerImpl();
        lab.setOid("xyz");
        assertEquals("xyz", lab.getOid());
        lab.setOid("abc");
        assertEquals("abc", lab.getOid());

    }


    @Test
    public void testEqualityAndHashCode() {

        Painting px = new PaintingImpl();
        px.setOid( "oidX" );
        px.setHasComment("Some comment on this object");

        assertEquals( px, painting );
        assertFalse( px.hashCode() == painting.hashCode() );



        px.setHasComment("Change value");

        assertFalse( px.equals( painting ) );
        assertFalse( px.hashCode() == painting.hashCode() );


        painting.setDyEntryId( "aid" );
        ((PaintingImpl) painting).setDyReference( false );
        px.setDyEntryId( "aid" );
        ((PaintingImpl) px).setDyReference( false );

        assertEquals( painting, px );
        assertTrue( px.hashCode() == painting.hashCode() );


        px.setDyEntryId( "aid2" );

        assertFalse( px.equals( painting ) );
        assertFalse( px.hashCode() == painting.hashCode() );

    }




    private Marshaller createMarshaller() {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance( factory.getClass().getPackage().getName() );
            Marshaller marsh = jaxbContext.createMarshaller();
                marsh.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
                marsh.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            return marsh;
        } catch ( JAXBException e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        return null;
    }

    private Unmarshaller createUnMarshaller() {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance( factory.getClass().getPackage().getName() );
            Unmarshaller unmarsh = jaxbContext.createUnmarshaller();
            return unmarsh;
        } catch ( JAXBException e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        return null;
    }


    private Object refreshOnJaxb( Object o ) throws JAXBException {
        StringWriter writer;

        writer = new StringWriter();
        createMarshaller().marshal( o, writer );
        String initial = writer.toString();
        System.err.println( initial );

        System.err.println( "------------------------------------------" );

        Object ret = createUnMarshaller().unmarshal( new StringReader( writer.toString() ) );


        writer = new StringWriter();
        createMarshaller().marshal( ret, writer );
        String fineal = writer.toString();
        System.err.println( fineal );

        assertEquals( initial, fineal );
        return ret;
    }






    private void persist( Object o, EntityManager em ) {
        em.getTransaction().begin();
        em.persist( o );
        em.getTransaction().commit();

        em.clear();

    }

    private Object refreshOnJPA( Object o, Object key, EntityManager em ) {

        Object ans = null;

        em.getTransaction().begin();
        ans = em.find( o.getClass(), key );
        em.getTransaction().commit();


        return ans;
    }






    @Test
    public void testEmpireInheritance() {

        Empire.init(new OpenRdfEmpireModule());
        EmpireOptions.STRICT_MODE = false;

        PersistenceProvider aProvider = Empire.get().persistenceProvider();

        EntityManagerFactory emf = aProvider.createEntityManagerFactory( ObjectFactory.class.getPackage().getName()
                , getTestEMConfigMap() );
        EntityManager em = emf.createEntityManager();




        IronInstallation ironi = new IronInstallationImpl();
        Smith smith = new SmithImpl();
        Labourer labrr = new LabourerImpl();
        Guest guest = new GuestImpl();

        ironi.getInvolves().add( smith );
        ironi.getInvolves().add( labrr );
        ironi.getInvolves().add( guest );

        persist( ironi, em );

        System.out.println( "IronI has id " + ironi.getRdfId() );
        System.out.println( "Smith has id " + smith.getRdfId() );
        System.out.println( "Labrr has id " + labrr.getRdfId() );
        System.out.println( "Guest has id " + guest.getRdfId() );



        IronInstallation ironBuild = (IronInstallation) refreshOnJPA( ironi, ironi.getRdfId(), em );

        assertEquals( 3, ironBuild.getInvolves().size() );

        boolean x1 = false;
        boolean x2 = false;
        boolean x3 = false;
        boolean x4 = false;
        GuestImpl newGuest = null;

        for( Person p : ironBuild.getInvolves() ) {
            if ( p instanceof Smith ) x1 = true;
            if ( p instanceof Labourer ) x2 = true;
            if ( p instanceof Guest ) x3 = true;

            if ( p instanceof GuestImpl ) {
                x4 = true;
                newGuest = (GuestImpl) p;
            }
        }

        assertTrue( x1 && x2 && x3 && x4 );
        assertNotNull( newGuest );

        newGuest.withOid("abc");
        assertTrue( newGuest.getOid().contains( "abc" ) );
    }











    private static Map<String, String> getTestEMConfigMap() {
        Map<String, String> aMap = new HashMap<String, String>();

        aMap.put(ConfigKeys.FACTORY, RepositoryDataSourceFactory.class.getName());
        aMap.put(RepositoryDataSourceFactory.REPO, "test-repo");
        aMap.put(RepositoryDataSourceFactory.FILES, "");
        aMap.put(RepositoryDataSourceFactory.QUERY_LANG, RepositoryDataSourceFactory.LANG_SERQL);

        return aMap;
    }



    @Test
    public void testEmpire() {
        Empire.init(new OpenRdfEmpireModule());
        EmpireOptions.STRICT_MODE = false;

        PersistenceProvider aProvider = Empire.get().persistenceProvider();

        EntityManagerFactory emf = aProvider.createEntityManagerFactory( ObjectFactory.class.getPackage().getName()
                , getTestEMConfigMap() );
        EntityManager em = emf.createEntityManager();

        persist( painting, em );

        Painting p2 = (Painting) refreshOnJPA( painting, painting.getRdfId(), em );

        assertTrue( p2 instanceof Painting );
        assertTrue( p2 instanceof PaintingImpl );
        assertEquals(  new Integer(10), p2.getRequiresStair().getStairLength() );

        assertEquals( painting, p2 );
        assertEquals( p2, painting );

        p2.setHasComment(" Change my mind ");
        p2.getRequiresStair().setStairLength(6);

        assertTrue( painting.equals( p2 ) );
        assertNotSame( painting, p2 );

        PaintingImpl p3 = new PaintingImpl();

        p3.mergeFrom( p3, p2 );

        checkPainting( p2 );
        checkPainting( p3 );

        em.close();
    }


    @Test
    public void testRuleWithEmpire() {

        Empire.init(new OpenRdfEmpireModule());
        EmpireOptions.STRICT_MODE = false;

        PersistenceProvider aProvider = Empire.get().persistenceProvider();

        EntityManagerFactory emf = aProvider.createEntityManagerFactory( ObjectFactory.class.getPackage().getName()
                , getTestEMConfigMap() );
        EntityManager em = emf.createEntityManager();

        persist( painting, em );

        Painting paintingFact = (Painting) refreshOnJPA( painting, painting.getRdfId(), em );




        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        builder.add( new ClassPathResource( "testFacts.drl" ), ResourceType.DRL );
        if ( builder.hasErrors() ) {
            fail( builder.getErrors().toString() );
        }

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( builder.getKnowledgePackages() );

        ArrayList ans = new ArrayList();
        StatefulKnowledgeSession kSession = kBase.newStatefulKnowledgeSession();
        kSession.setGlobal( "ans", ans );

        kSession.insert(paintingFact);

        kSession.fireAllRules();

        assertEquals( 2, ans.size() );

        System.out.println( ans );

    }




    @Test
    public void testJPA() throws ClassNotFoundException {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
                "org.drools.owl.conyard:org.w3._2002._07.owl"
        );
        EntityManager em = emf.createEntityManager();



        persist(painting, em);

        Painting p2 = (Painting) refreshOnJPA( painting, (painting).getDyEntryId(), em );


        checkPainting( p2 );

        em.close();

    }




    @Test
    @Ignore
    public void validateXMLWithSchema() throws SAXException {
        StringWriter writer = new StringWriter();

        try {
            createMarshaller().marshal( painting, writer );
        } catch (JAXBException e) {
            fail( e.getMessage() );
        }

        String inXSD = "org.drools.owl.conyard.xsd";
        String global = "owlThing.xsd";

        SchemaFactory factory =
                SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );

        Source schemaFile = null;
        Source globalFile = null;
        try {
            globalFile = new StreamSource( new ClassPathResource( global ).getInputStream() );
            schemaFile = new StreamSource( new ClassPathResource( inXSD ).getInputStream() );
        } catch ( IOException e ) {
            fail( e.getMessage() );
        }
        Schema schema = factory.newSchema( new Source[] { globalFile, schemaFile } );


        System.out.println( writer.toString() );

        Validator validator = schema.newValidator();

        Source source = new StreamSource( new ByteArrayInputStream( writer.toString().getBytes() ) );

        try {
            validator.validate(source);
        }
        catch ( SAXException ex ) {
            fail( ex.getMessage() );
        } catch ( IOException ex ) {
            fail( ex.getMessage() );
        }

    }


    @Test
    public void testBasicProperties() {
        Empire.init(new OpenRdfEmpireModule());
        EmpireOptions.STRICT_MODE = false;

        PersistenceProvider aProvider = Empire.get().persistenceProvider();

        EntityManagerFactory emf = aProvider.createEntityManagerFactory( ObjectFactory.class.getPackage().getName()
                , getTestEMConfigMap() );
        EntityManager em = emf.createEntityManager();

        Stair st = new StairImpl();
        st.setStairLength( 10 );
        persist( st, em );

        Stair s2 = (Stair) refreshOnJPA( st, st.getRdfId(), em );

        assertEquals( new Integer(10), s2.getStairLength() );


        em.close();
    }

    @Test
    public void testCascadedBasicProperties() {
        Empire.init(new OpenRdfEmpireModule());
        EmpireOptions.STRICT_MODE = false;

        PersistenceProvider aProvider = Empire.get().persistenceProvider();

        EntityManagerFactory emf = aProvider.createEntityManagerFactory( ObjectFactory.class.getPackage().getName()
                , getTestEMConfigMap() );
        EntityManager em = emf.createEntityManager();

        Painting p = new PaintingImpl();
        Stair st = new StairImpl();
        st.setStairLength( 10 );
        p.addRequires( st );

        persist( p, em );

        Painting p2 = (Painting) refreshOnJPA( p, p.getRdfId(), em );

        assertEquals( new Integer(10), p.getRequiresStair().getStairLength() );


        em.close();
    }


    @Test
    public void testXMLNamespaces() {
        StringWriter writer;

        writer = new StringWriter();
        try {
            createMarshaller().marshal( painting, writer );
        } catch (JAXBException e) {
            fail( e.getMessage() );
        }

        try {
            System.out.println( writer.toString() );
            Document dox = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new ByteArrayInputStream( writer.toString().getBytes() ) );
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression xPathExpression = xPath.compile("//namespace::*" );
            NodeList nodeList = (NodeList) xPathExpression.evaluate( dox, XPathConstants.NODESET );
            for ( int j = 0; j < nodeList.getLength(); j++ ) {
                Node n = nodeList.item( j );
                if ( n.getNodeName().equals( "xmlns" ) ) {
                    assertEquals( "http://owl.drools.org/conyard", n.getNodeValue() );
                } else if ( n.getNodeName().equals( "xmlns:xml" ) ) {
                    assertEquals( "http://www.w3.org/XML/1998/namespace", n.getNodeValue() );
                } else if ( n.getNodeName().equals( "xmlns:xsi" ) ) {
                    assertEquals( "http://www.w3.org/2001/XMLSchema-instance", n.getNodeValue() );
                } else if ( n.getNodeName().equals( "xmlns:xs" ) ) {
                    assertEquals( "http://www.w3.org/2001/XMLSchema", n.getNodeValue() );
                } else {
                    fail( "Unexpected namespace " + n.getNodeName() + " :: " + n.getNodeValue() );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }


    @Test
    public void testJaxbFromStringEmptyContext() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<tns:Painting xs:id=\"http://0e1dfb13-3828-4922-be3d-d4a6fde37233\" xmlns:tns=\"http://owl.drools.org/conyard\" xmlns:owl=\"http://www.w3.org/2002/07/owl\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <owl:dyReference>false</owl:dyReference>\n" +
                "    <tns:oid>oidX</tns:oid>\n" +
                "    <tns:endsOn>2013-02-02T18:51:03Z</tns:endsOn>\n" +
                "    <tns:requires xsi:type=\"tns:Stair\" xs:id=\"http://4feb2921-4375-4a46-8eff-63811154a34b\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>stairId</tns:oid>\n" +
                "        <tns:stairLength>10</tns:stairLength>\n" +
                "    </tns:requires>\n" +
                "    <tns:requires xsi:type=\"tns:Paint\" xs:id=\"http://586f2d5f-156b-40b2-bef4-cfcd8f399a9a\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>R0_G0_B0</tns:oid>\n" +
                "        <tns:storedIn xs:id=\"http://eea048dd-a152-4e54-b6b9-6d916edc3b60\">\n" +
                "            <owl:dyReference>false</owl:dyReference>\n" +
                "            <tns:oid>siteId</tns:oid>\n" +
                "            <tns:radius>100.0</tns:radius>\n" +
                "            <tns:centerY>20.0</tns:centerY>\n" +
                "            <tns:centerX>10.0</tns:centerX>\n" +
                "        </tns:storedIn>\n" +
                "    </tns:requires>\n" +
                "    <tns:requires xs:id=\"http://e1c736d6-3fe5-4538-97c7-c4ab678adf4b\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>ekwip1</tns:oid>\n" +
                "    </tns:requires>\n" +
                "    <tns:involves xs:id=\"http://6a37da4c-bf8f-434d-bcd2-5e7583f88c83\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>aGuy</tns:oid>\n" +
                "    </tns:involves>\n" +
                "    <tns:involves xsi:type=\"tns:Labourer\" xs:id=\"http://9f7ab849-7b9b-4910-947d-c786ae08273a\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>123456x</tns:oid>\n" +
                "    </tns:involves>\n" +
                "    <tns:involves xsi:type=\"tns:Labourer\" xs:id=\"http://81fdbed7-3d37-41e2-a1d4-10583ee1c851\">\n" +
                "        <owl:dyReference>false</owl:dyReference>\n" +
                "        <tns:oid>pers2</tns:oid>\n" +
                "        <tns:participatesIn xsi:type=\"tns:Painting\" xs:id=\"http://0e1dfb13-3828-4922-be3d-d4a6fde37233\">\n" +
                "            <owl:dyReference>false</owl:dyReference>\n" +
                "        </tns:participatesIn>\n" +
                "    </tns:involves>\n" +
                "    <tns:requiresAlso xsi:type=\"tns:Stair\" xs:id=\"http://4feb2921-4375-4a46-8eff-63811154a34b\">\n" +
                "        <owl:dyReference>true</owl:dyReference>\n" +
                "    </tns:requiresAlso>\n" +
                "    <tns:startsOn>2013-02-02T18:51:03Z</tns:startsOn>\n" +
                "    <tns:hasComment>Some comment on this object</tns:hasComment>\n" +
                "</tns:Painting>";

        try {
            Object paint2 = createUnMarshaller().unmarshal( new StringReader( xml ) );
            checkPainting( (Painting) paint2 );
        } catch ( JAXBException jxe ) {
            jxe.printStackTrace();
            fail(jxe.getMessage());
        }
    }

}