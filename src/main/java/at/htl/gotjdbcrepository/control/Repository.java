package at.htl.gotjdbcrepository.control;

import at.htl.gotjdbcrepository.entity.Person;

public interface Repository {
    public Person save(Person p);
    public void delete(long id);
}
