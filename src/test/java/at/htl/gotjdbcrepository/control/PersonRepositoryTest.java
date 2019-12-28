package at.htl.gotjdbcrepository.control;

import at.htl.gotjdbcrepository.entity.Person;
import org.apache.derby.jdbc.ClientDataSource;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static at.htl.gotjdbcrepository.control.PersonRepository.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.db.output.Outputs.output;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class PersonRepositoryTest {

    private static final String HUGE_FILE = "got.csv";
    private static final String SMALL_FILE = "got2.csv";
    //private static Source source;
    private static DataSource dataSource;

    @BeforeAll
    private static void init() {
        //source = new Source(URL, USERNAME, PASSWORD);
        dataSource = getDatasource();
    }

    @BeforeEach
    private void initEach() {
        deleteAllFromTable(TABLE_NAME);
    }


    /**
     * Ein Repository wird erstellt. Es wird überprüft, ob ein Objekt erstellt wird
     * <p>
     * Annhame: die Tabelle exisitiert noch vom letzten Test
     */
    @Test
    void test010_createRepository() {
        PersonRepository personRepository = getInstance();
        assertThat(personRepository).isNotNull();
    }

    @Test
    void test020_checkIfSingletonWorks() {
        PersonRepository personRepository1 = getInstance();
        PersonRepository personRepository2 = getInstance();
        System.out.println(personRepository1);
        System.out.println(personRepository2);
        assertThat(personRepository1 == personRepository2).isTrue();
        assertThat(personRepository1).isNotNull();

    }

    @Test
    void test030_createRepositoryWhenTableExisting() {
        // create a table (if not already existing)
        PersonRepository personRepository = getInstance();

        // die Variable "instance" in der Klasse PersonRepository wird auf null gesetzt
        setRepositoryInstanceToNull();
        assertThat(tableExists(TABLE_NAME)).isTrue();

        // erstelle neuerlich eine Repository-Instanz
        personRepository = getInstance();
        assertThat(personRepository).isNotNull();
        assertThat(tableExists(TABLE_NAME)).isTrue();
    }

    /**
     * Falls noch keine Tabelle PERSON existiert, muss das Objekt personRepository eine Tabelle (automatisch) erstellen
     */
    @Test
    void test040_createRepositoryWhenTableNotExisting() {
        dropTable(TABLE_NAME);
        // die Variable "instance" in der Klasse PersonRepository wird auf null gesetzt
        setRepositoryInstanceToNull();
        PersonRepository personRepository = getInstance();
        assertThat(tableExists(TABLE_NAME)).isTrue();
    }


    /**
     * Ein personRepository-Objekt wird erstellt.
     * Prüfen der Methode "deleteAll()"
     */
    @Test
    void test050_createRepositoryAndDeleteAllRecords() {

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();

        deleteAllFromTable(TABLE_NAME);

        insertPerson("Gariss", "Tyria", "Westbrook");

        System.out.println("\nAfter inserting one Person ");
        personTable = new Table(dataSource, TABLE_NAME);  // aktualisieren des table-Objekts
        output(personTable).toConsole();

        PersonRepository personRepository = getInstance();
        personRepository.deleteAll();

        System.out.println("\nAfter deleting one Person with deleteAll()");
        personTable = new Table(dataSource, TABLE_NAME);  // aktualisieren des table-Objekts
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).isEmpty();
    }

    @Test
    void test055_EqualityOfTwoPersons() {
        Person p1 = new Person("a", "b", "c");
        p1.setId(1L);
        Person p2 = new Person("a", "b", "c");
        p1.setId(2L);
        assertThat(p1).isEqualTo(p2);
    }

    /**
     * This test is true, because it tests non-equality
     */
    @Test
    void test055_NonEqualityOfTwoPersons() {
        Person p1 = new Person("a", "b1", "c1");
        p1.setId(1L);
        Person p2 = new Person("a", "b2", "c2");
        p1.setId(1L);
        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void test060_checkDeleteAllRecords() {

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();

        insertPerson("Gariss", "Tyria", "Westbrook");
        insertPerson("Garizon", "Lorath", "Lake");
        insertPerson("Mark Mullendore", "Asshai", "Longthorpe of Longsister");

        System.out.println("\nAfter inserting one Person ");
        personTable = new Table(dataSource, TABLE_NAME);  // aktualisieren des table-Objekts
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(3);

        PersonRepository personRepository = getInstance();
        personRepository.deleteAll();

        System.out.println("\nAfter deleting one Person with deleteAll()");
        personTable = new Table(dataSource, TABLE_NAME);  // aktualisieren des table-Objekts
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).isEmpty();
    }

    /**
     * Eine Person wird gespeichert. Es wird überprüft, ob ein Personenobjekt mit aktuelle ID
     * zurückgegeben wird
     */
    @Test
    void test070_insertRecord() {
        // zurücksetzen der von der DB generierten ID auf 1
        dropTable(TABLE_NAME);
        setRepositoryInstanceToNull();

        Person jakob = new Person("Jakob", "Bad Leonfelden", "Targaryen");

        PersonRepository personRepository = getInstance();
        Person savedJakob = personRepository.save(jakob);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(1);
        assertThat(jakob).isEqualTo(savedJakob);
        assertThat(savedJakob.getId()).isEqualTo(1L);
        org.assertj.db.api.Assertions.assertThat(personTable).row(0).hasValues(1L, "Jakob", "Bad Leonfelden", "Targaryen");
    }

    /**
     * doppelte Namen sind erlaubt
     */
    @Test
    void test080_saveTwoRecords() {
        Person missandei1 = new Person("Missandei", "Asshai", "Longthorpe of Longsister");
        Person missandei2 = new Person("Missandei", "Lorath", "Lyberr");

        PersonRepository personRepository = getInstance();
        personRepository.save(missandei1);
        personRepository.save(missandei2);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(2);
    }

    /**
     * Es wird versucht eine Person, 2 x zu speichern. Dies darf nicht gelingen (siehe Angabe).
     * Nicht erlaubt sind doppelte Zeilen mit identen NAME, CITY und HOUSE
     */
    @Test
    void test090_saveDuplicateRecord() {
        Person jakob = new Person("Jakob", "White Harbour", "Targaryen");

        PersonRepository personRepository = getInstance();
        personRepository.save(jakob);
        personRepository.save(jakob);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(1);
    }

    /**
     *
     */
    @Test
    void test094_saveWithNonExistingId() {
        /**
         * arrange ... Vorbereiten der Testsituation
         */
        // zurücksetzen der von der DB generierten ID auf 1
        dropTable(TABLE_NAME);
        setRepositoryInstanceToNull();

        Person jakob = new Person("Jakob", "White Harbour", "Targaryen");
        jakob.setId(1000L);

        /**
         * act ... Durchführen des Tests
         */
        PersonRepository personRepository = getInstance();
        Person savedJakob = personRepository.save(jakob);

        /**
         * assert ... Ergebnisse kontrollieren
         */
        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(1);
        org.assertj.db.api.Assertions.assertThat(personTable).row(0).hasValues(1L, "Jakob", "White Harbour", "Targaryen");
    }

    /**
     *
     */
    @Test
    void test100_findById() {

        /**
         * arrange ... Vorbereiten der Testsituation
         */
        // zurücksetzen der von der DB generierten ID auf 1
        dropTable(TABLE_NAME);
        setRepositoryInstanceToNull();

        // einlesen der Testdaten
        List<Person> persons = readCsv(HUGE_FILE, 50);
        PersonRepository personRepository = getInstance();
        // in db speichern
        persons.stream().forEach(personRepository::save);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(50);

        Person expectedPerson = new Person("Lord Ashford", "Oros", "Lanny");
        expectedPerson.setId(10L);

        /**
         * act ... Durchführen des Tests
         */
        Person actualPerson = personRepository.find(10);

        /**
         * assert ... Ergebnisse kontrollieren
         */
        assertThat(actualPerson).isEqualTo(expectedPerson);
        assertThat(actualPerson.getId()).isEqualTo(expectedPerson.getId());
    }

    @Test
    void test110_deleteAll() {
        /**
         * arrange ... Vorbereiten der Testsituation
         */
        // einlesen der Testdaten
        List<Person> persons = readCsv(HUGE_FILE, 50);
        PersonRepository personRepository = getInstance();
        // in db speichern
        persons.stream().forEach(personRepository::save);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(50);

        /**
         * act ... Durchführen des Tests
         */
        personRepository.deleteAll();

        /**
         * assert ... Ergebnisse kontrollieren
         */
        personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).isEmpty();
    }

    @Test
    void test120_findByHouse() {
        /**
         * arrange ... Vorbereiten der Testsituation
         */
        // zurücksetzen der von der DB generierten ID auf 1
        dropTable(TABLE_NAME);
        setRepositoryInstanceToNull();

        // einlesen der Testdaten
        List<Person> persons = readCsv(HUGE_FILE, 200);
        PersonRepository personRepository = getInstance();
        // in db speichern
        persons.stream().forEach(personRepository::save);

        Table personTable = new Table(dataSource, TABLE_NAME);
        output(personTable).toConsole();
        org.assertj.db.api.Assertions.assertThat(personTable).hasNumberOfRows(200);

        persons.stream().sorted(Comparator.comparing(Person::getHouse)).map(Person::getHouse).forEach(System.out::println);

        /**
         * act ... Durchführen des Tests
         */
        List<Person> actualPersons = personRepository.findByHouse("Stane of Driftwood Hall");
        actualPersons.stream().forEach(System.out::println);

        /**
         * assert ... Ergebnisse kontrollieren
         */
        assertThat(actualPersons).hasSize(4);

        // Variante 1
        assertThat(actualPersons)
                .extracting("name", "city", "house")
                .contains(
                        tuple("Timon", "Norvos", "Stane of Driftwood Hall"),
                        tuple("Norbert Vance", "Myr", "Stane of Driftwood Hall"),
                        tuple("Androw Ashford", "Lorath", "Stane of Driftwood Hall"),
                        tuple("The Great Walrus", "Braavos", "Stane of Driftwood Hall")
                );

        // Variante 2
        assertThat(actualPersons).contains(
                new Person("Timon", "Norvos", "Stane of Driftwood Hall"),
                new Person("Norbert Vance", "Myr", "Stane of Driftwood Hall"),
                new Person("Androw Ashford", "Lorath", "Stane of Driftwood Hall"),
                new Person("The Great Walrus", "Braavos", "Stane of Driftwood Hall")
        );
    }


    /*













     */

    /**
     *
     * IM UNTEREN BEREICH SIND NUR METHODEN, DIE FÜR DIE DURCHFÜHRUNG DER UNIT-TESTS BENÖTIGT WERDEN.
     * HIER WERDEN TECHNIKEN VERWENDET, DIE WIR NOCH NICHT GELERNT HABEN UND DAHER AUCH NICHT TESTRELEVANT SIND.
     *
     */



    /**
     * Eine weitere Möglichkeit auf eine Datenbank zuzugreifen ist neben dem bereits  bekannten
     * try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) { ... }
     * (also der Zugriff auf den DriverManager),
     * eine sogenannte Datasource zu erstellen (dies ist auch die empfohlene Methode)
     * <p>
     * Beide Methoden funktionieren gleich gut. Habe Sie hier verwendet, da AssertJ-DB DataSources verlangt
     * <p>
     * Zu beachten ist, dass wir hier ein für alle Datenbanktypen gültiges Interface zurückgeben, aber
     * eine Derby-spezifische ClientDatasource-Klasse instanzieren.
     *
     * @return javax.sql.DataSource
     */
    private static DataSource getDatasource() {
        ClientDataSource dataSource = new ClientDataSource();
        dataSource.setServerName("localhost");   // ist default Wert
        dataSource.setDatabaseName(DATABASE);
        dataSource.setUser(USERNAME);
        dataSource.setPassword(PASSWORD);
        return dataSource;
    }

    private void deleteAllFromTable() {
        deleteAllFromTable(TABLE_NAME);
    }

    private void deleteAllFromTable(final String table) {
        try (Connection conn = dataSource.getConnection()) {
            // Inhalt der Tabelle löschen
            String sql = "DELETE FROM " + table;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            final int rowsAffected = pstmt.executeUpdate();
            System.out.println(rowsAffected + " Zeile(n) wurde(n) gelöscht");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }


    private void insertPerson(final String name, final String city, final String house) {
        try (Connection conn = dataSource.getConnection()) {
            // Eine Zeile in Tabelle einfügen
            String sql = "INSERT INTO person (name, city, house) VALUES (?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, city);
            pstmt.setString(3, house);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private void dropTable(final String table) {
        try (Connection conn = dataSource.getConnection()) {
            // Tabellenstruktur löschen
            String sql = "DROP TABLE " + table;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            if (tableExists(table)) {
                fail("Tabelle " + table.toUpperCase() + " still exists; DROP didn't work");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * Hier werden DatabaseMetaData verwendet - das haben wir noch nicht gelernt und wird nur von
     * mir für die Tests verwendet
     *
     * @param table
     * @return
     * @throws SQLException
     */
    private boolean tableExists(String table) {
        try (Connection conn = dataSource.getConnection()) {
            // Verwendung von Metadata, um zu kontrollieren, ob Tabelle auch wirklich gelöscht wurde
            DatabaseMetaData databaseMetaData = conn.getMetaData();
            try (ResultSet rs = databaseMetaData.getTables(null, null, table.toUpperCase(),
                    new String[]{"TABLE"});) {
                if (rs.next()) {
//                System.out.println(
//                        "   "+rs.getString("TABLE_CAT")
//                                + ", "+rs.getString("TABLE_SCHEM")
//                                + ", "+rs.getString("TABLE_NAME")
//                                + ", "+rs.getString("TABLE_TYPE")
//                                + ", "+rs.getString("REMARKS"));
                    System.err.println("Tabelle " + table.toUpperCase() + " exists");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("Tabelle " + table.toUpperCase() + " dropped");
        return false;
    }

    /**
     * Hier wird Reflection verwendet, um auf ein privates Feld zugreifen zu können,
     * was wir ebenfalls noch nicht gelernt haben
     * <p>
     * http://tutorials.jenkov.com/java-reflection/index.html
     * https://javahowtodoit.wordpress.com/2014/09/12/how-to-get-and-set-private-static-final-field-using-java-reflection/
     */
    private void setRepositoryInstanceToNull() {
        try {
            Field instance = PersonRepository.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
    }

    private List<Person> readCsv(String fileName, int numberOfLines) {
        List<Person> persons = new LinkedList<>();

        List<String> lines = null;
        try {
            Files
                    .readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)
                    .stream()
                    .skip(1)
                    .limit(numberOfLines)
                    .peek(System.out::println)
                    .map(line -> line.split(";"))
                    .map(elements -> new Person(elements[0], elements[1], elements[2]))
                    //.distinct()
                    .forEach(persons::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persons;
    }

}