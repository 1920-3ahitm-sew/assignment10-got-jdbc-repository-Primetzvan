package at.htl.gotjdbcrepository.entity;

import java.util.Objects;

public class Person {

    private Long id;
    private String name;
    private String city;
    private String house;

    public Person() {
    }

    public Person(String name, String city, String house) {
        this.name = name;
        this.city = city;
        this.house = house;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }



    @Override
    public String toString() {
        return String.format("%s, %s, %s", name, city, house);
    }
}
