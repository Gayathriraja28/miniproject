import java.sql.*;
import java.util.*;

class Movie {
    private int id;
    private String title;
    private String genre;
    private boolean available;

    public Movie(int id, String title, String genre, boolean available) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.available = available;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public boolean isAvailable() {
        return available;
    }
}

class Customer {
    private int id;
    private String name;

    public Customer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

class Rental {
    private int id;
    private Movie movie;
    private Customer customer;
    private Date rentalDate;
    private Date returnDate;

    public Rental(int id, Movie movie, Customer customer, Date rentalDate, Date returnDate) {
        this.id = id;
        this.movie = movie;
        this.customer = customer;
        this.rentalDate = rentalDate;
        this.returnDate = returnDate;
    }

    public int getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Date getRentalDate() {
        return rentalDate;
    }

    public Date getReturnDate() {
        return returnDate;
    }
}

class RentalSystem {
    private Connection connection;

    public RentalSystem(Connection connection) {
        this.connection = connection;
    }

    public List<Movie> getAllMovies() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String query = "SELECT * FROM movies";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String title = resultSet.getString("title");
            String genre = resultSet.getString("genre");
            boolean available = resultSet.getBoolean("available");
            Movie movie = new Movie(id, title, genre, available);
            movies.add(movie);
        }

        resultSet.close();
        statement.close();

        return movies;
    }

    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            Customer customer = new Customer(id, name);
            customers.add(customer);
        }

        resultSet.close();
        statement.close();

        return customers;
    }

    public void rentMovie(int movieId, int customerId) throws SQLException {
        String rentQuery = "UPDATE movies SET available = false WHERE id = ?";
        String rentalQuery = "INSERT INTO rentals (movie_id, customer_id, rental_date) VALUES (?, ?, ?)";

        PreparedStatement rentStatement = connection.prepareStatement(rentQuery);
        rentStatement.setInt(1, movieId);
        rentStatement.executeUpdate();
        rentStatement.close();

        PreparedStatement rentalStatement = connection.prepareStatement(rentalQuery);
        rentalStatement.setInt(1, movieId);
        rentalStatement.setInt(2, customerId);
        rentalStatement.setDate(3, new java.sql.Date(System.currentTimeMillis()));
        rentalStatement.executeUpdate();
        rentalStatement.close();
    }

    public void returnMovie(int rentalId) throws SQLException {
        String returnQuery = "UPDATE movies SET available = true WHERE id = (SELECT movie_id FROM rentals WHERE id = ?)";
        String rentalQuery = "UPDATE rentals SET return_date = ? WHERE id = ?";

        PreparedStatement returnStatement = connection.prepareStatement(returnQuery);
        returnStatement.setInt(1, rentalId);
        returnStatement.executeUpdate();
        returnStatement.close();

        PreparedStatement rentalStatement = connection.prepareStatement(rentalQuery);
        rentalStatement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
        rentalStatement.setInt(2, rentalId);
        rentalStatement.executeUpdate();
        rentalStatement.close();
    }

    public List<Rental> getRentalsByCustomer(int customerId) throws SQLException {
        List<Rental> rentals = new ArrayList<>();
        String query = "SELECT r.id, r.rental_date, r.return_date, m.id, m.title, m.genre " +
                "FROM rentals r " +
                "JOIN movies m ON r.movie_id = m.id " +
                "WHERE r.customer_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, customerId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            int rentalId = resultSet.getInt(1);
            Date rentalDate = resultSet.getDate(2);
            Date returnDate = resultSet.getDate(3);
            int movieId = resultSet.getInt(4);
            String movieTitle = resultSet.getString(5);
            String movieGenre = resultSet.getString(6);
            Movie movie = new Movie(movieId, movieTitle, movieGenre, true);
            Customer customer = new Customer(customerId, "");
            Rental rental = new Rental(rentalId, movie, customer, rentalDate, returnDate);
            rentals.add(rental);
        }

        resultSet.close();
        statement.close();

        return rentals;
    }
}

public class Main {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/rental_system";
        String username = "root";
        String password = "password";

        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            RentalSystem rentalSystem = new RentalSystem(connection);

            List<Movie> movies = rentalSystem.getAllMovies();
            System.out.println("All Movies:");
            for (Movie movie : movies) {
                System.out.println("ID: " + movie.getId() + ", Title: " + movie.getTitle() +
                        ", Genre: " + movie.getGenre() + ", Available: " + movie.isAvailable());
            }

         
            List<Customer> customers = rentalSystem.getAllCustomers();
            System.out.println("\nAll Customers:");
            for (Customer customer : customers) {
                System.out.println("ID: " + customer.getId() + ", Name: " + customer.getName());
            }

            
            int movieId = 1; 
            int customerId = 1; 
            rentalSystem.rentMovie(movieId, customerId);
            System.out.println("\nMovie rented successfully!");

            
            int rentalId = 1; 
            rentalSystem.returnMovie(rentalId);
            System.out.println("Movie returned successfully!");

         
            int customerIdToQuery = 1; 
            List<Rental> rentals = rentalSystem.getRentalsByCustomer(customerIdToQuery);
            System.out.println("\nRentals for Customer ID " + customerIdToQuery + ":");
            for (Rental rental : rentals) {
                System.out.println("Rental ID: " + rental.getId() +
                        ", Movie: " + rental.getMovie().getTitle() +
                        ", Rental Date: " + rental.getRentalDate() +
                        ", Return Date: " + rental.getReturnDate());
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
