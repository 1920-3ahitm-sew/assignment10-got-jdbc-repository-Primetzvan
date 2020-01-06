package at.htl.gotjdbcrepository.control;

import at.htl.gotjdbcrepository.entity.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PersonRepository implements Repository {
    public static final String USERNAME = "app";
    public static final String PASSWORD = "app";
    public static final String DATABASE = "db";
    public static final String URL = "jdbc:derby://localhost:1527/" + DATABASE + ";create=true";
    public static final String TABLE_NAME = "person";

    private static PersonRepository instance;

    private PersonRepository() {

        createTable();

    }

    public static synchronized PersonRepository getInstance() {

        if (instance == null) {
            instance = new PersonRepository();
        }

        return instance;
    }

    private void createTable() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT " + TABLE_NAME + "_pk PRIMARY KEY," +
                        "name VARCHAR(255)," +
                        "city VARCHAR(255)," +
                        "house VARCHAR(255)," +
                        "CONSTRAINT " + TABLE_NAME + "_uq UNIQUE (name, city, house)" +
                        ")";
                stmt.executeUpdate(sql);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            //System.err.format("SQL State: %s - %s\n", e.getSQLState(), e.getMessage());
        }
    }

    public void deleteAll() {

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String query = "DELETE FROM " + TABLE_NAME;
                int deletedRows = stmt.executeUpdate(query);
                if (deletedRows > 0) {
                    System.out.println("Deleted All Rows In The Table Successfully...");
                } else {
                    System.out.println("Table already empty.");
                }

            } catch (SQLException s) {
                System.out.println("Deleted All Rows In  Table Error. ");
                s.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * Hat newPerson eine ID (id != null) so in der Tabelle die entsprechende Person gesucht und upgedated
     * Hat newPerson keine ID wird ein neuer Datensatz eingefügt.
     * <p>
     * Wie man die generierte ID zurück erhält: https://stackoverflow.com/a/1915197
     * <p>
     * Falls ein Fehler auftritt, wird nur die Fehlermeldung ausgegeben, der Programmlauf nicht abgebrochen
     * <p>
     * Verwenden sie hier die privaten MEthoden update() und insert()
     *
     * @param newPerson
     * @return die gespeicherte Person mit der (neuen) id
     */
    @Override
    public Person save(Person newPerson) {

        if (newPerson.getId() != null) {

            update(newPerson);
            return newPerson;

        }

        return insert(newPerson);


    }

    /**
     * Wie man die generierte ID erhält: https://stackoverflow.com/a/1915197
     *
     * @param personToSave
     * @return Rückgabe der Person inklusive der neu generierten ID
     */
    private Person insert(Person personToSave) {

        try (
                Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                PreparedStatement statement = connection.prepareStatement("INSERT INTO APP.PERSON (name,city,house) values (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
        ) {
            statement.setString(1, personToSave.getName());
            statement.setString(2, personToSave.getCity());
            statement.setString(3, personToSave.getHouse());
            // ...

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    personToSave.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return personToSave;
    }

    /**
     * @param personToSave
     * @return wenn erfolgreich --> Anzahl der eingefügten Zeilen, also 1
     * wenn nicht erfolgreich --> -1
     */
    private int update(Person personToSave) {

        if (find(personToSave.getId()) == null) {
            insert(personToSave);
            return 1;

        }


        return -1;
    }

    @Override
    public void delete(long id) {

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String query = "DELETE FROM " + TABLE_NAME + " WHERE id = " + id;
                int deletedRows = stmt.executeUpdate(query);

            } catch (SQLException s) {
                System.out.println("Deleted All Rows In  Table Error. ");
                s.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }


    }


    /**
     * Finden Sie eine Person anhand Ihrer ID
     *
     * @param id
     * @return die gefundene Person oder wenn nicht gefunden wird null zurückgegeben
     */
    public Person find(long id) {

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String query = "SELECT * FROM " + TABLE_NAME;
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    if (id == rs.getLong("id")) {
                        Person p = new Person(rs.getString("name"), rs.getString("city"), rs.getString("house"));
                        p.setId(id);
                        return p;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return null;
    }

    /**
     * @param house Name des Hauses
     * @return Liste aller Personen des gegebenen Hauses
     */
    public List<Person> findByHouse(String house) {

        List<Person> persons = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {

                String query = "SELECT * FROM " + TABLE_NAME;
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {

                    String currentHouse = rs.getString("house");

                    if (house.equals(currentHouse)) {
                        persons.add(new Person(rs.getString("name"), rs.getString("city"), house));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return persons;
    }


}
