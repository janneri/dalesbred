Dalesbred
=========

Dalesbred is a breed of domestic sheep originating in the United Kingdom.

It is also a library that strives to make database access from Java nicer.
Dalesbred assumes that SQL is a great way to access database, but that JDBC
as an API is causes pain. Therefore it wraps JDBC with a set of helpers
while still providing access to low-level functionality.

Getting started
---------------

Adding Maven dependencies:

    <dependencies>
        <dependency>
            <groupId>fi.evident.dalesbred</groupId>
            <artifactId>dalesbred</artifactId>
            <version>0.1.5</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>evident-public-maven-repository</id>
            <url>http://maven.evident.fi/</url>
        </repository>
    </repositories>

Configuring the database connection
-----------------------------------

Using direct connection to database:

    Database db = Database.forUrlAndCredentials(
        "jdbc:postgresql://example-host/example-db", "login", "password");

Note that this performs no connection pooling and is therefore probably not your preferred way of configuring
the system in production.

For container provided data source:

    Database db = Database.forJndiDataSource("java:comp/env/jdbc/ExampleDb"")

For other data source:

    Database db = Database.forDataSource(myDataSource);

For _javax.inject_-compatible dependency injection container (e.g. Guice or Java EE 6), you
can just make sure that there's a [Provider](http://docs.oracle.com/javaee/6/api/javax/inject/Provider.html)
for DataSource around and then just inject your Database:

    @Inject Database db;

Finding stuff
-------------

Finding simple results consisting of just basic types is simple:

    List<Integer> newIds = db.findAll(Integer.class, "select id from department where created_date > ?", date);

To fetch results with multiple columns, you need a class with matching constructor:

    List<Department> departments = db.findAll(Department.class, "select id, name from department);

    ...

    public final class Department {
        private final int id;
        private final String name;

        public Department(int id, String name) {
            this.id = id;
            this.name = name;
        }

        ...
    }

You can also convert the results directly to a map:

    Map<Integer,String> namesByIds = db.findMap(Integer.class, String.class, "select id, name from department");

Alternatively, you can supply your own RowMapper or ResultSetProcessor-implementations in place
of the class and handle the result sets manually, but usually this should be unnecessary.

Updates
-------

Normal updates are straightforward, since we don't need do much work to map the results:

    int modifiedRows = db.update("delete from user where id=?", 42);

If you plan to return stuff from updates, they are queries as far as Dalesbred is concerned:

    int id = db.findUniqueInt("insert into department (name) values ('foo') returning id");


SqlQuery vs. query parameters
-----------------------------

All methods come in two variants: there's an implementation that takes an SqlQuery as a parameter and there's
another implementation that takes String and variable arguments of parameters. The latter is just convenience
method for the further, meaning that the following code fragments are identical in functionality:

    import static fi.evident.dalesbred.SqlQuery.query

    db.findAll(Department.class, query("select id, name from department where update_timestamp > ?", date));

    db.findAll(Department.class, "select id, name from department where update_timestamp > ?", date);

Normally you want to use the latter form, but every once in a while it's useful to be able to pass the query
around with its parameters. In those cases you'd want to use the latter form. An example is when you build
the query dynamically:

    db.findAll(Department.class, buildDepartmentQuery(form))
