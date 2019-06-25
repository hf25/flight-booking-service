import java.io.FileInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Runs queries against a back-end database
 */
public class Query
{
  private String configFilename;
  private Properties configProps = new Properties();

  private String jSQLDriver;
  private String jSQLUrl;
  private String jSQLUser;
  private String jSQLPassword;

  // DB Connection
  private Connection conn;
  private String usernameLogedIn;
  // Logged In User
  private String username; // customer username is unique
  private ArrayList<Integer> allfid;
  // Canned queries
  // login status. if true, an account logged in, otherwise, false
  private boolean logedin;
  private int iti;
 

  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  private static final String FIND_OUT_USERNAME = "Select count(*) as count from users where username=?";
  private PreparedStatement findOutUsernameStatement;

  private static final String INSERT_USER = "insert into users values(?,?,?)";
  private PreparedStatement insertUserStatement;

  private static final String CHECK_USER_EXISTS = "select username,password from users where username=?";
  private PreparedStatement checkUserExistsStatement;

  private static final String COUNT_DIRECT_FLIGHTS = "select count(*) as count from Flights where origin_city=? and dest_city=? and day_of_month=? and canceled=0";
  private PreparedStatement countDirectFlightsStatement;

  private static final String COUNT_INDIRECT_FLIGHTS = "select count(*) as count from flights as f1, flights as f2 where f1.origin_city=? and"
  +" f2.dest_city=? and f1.dest_city=f2.origin_city and f1.day_of_month=f2.day_of_month and f1.day_of_month=? and f1.canceled=0 and f2.canceled=0";
  private PreparedStatement countIndirectFlightsStatement;
  
  private static final String OUTPUT_DIRECT_FLIGHTS = "select top(?) day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price,fid,canceled from Flights "
  +"where canceled=0 and origin_city=? and dest_city=? and day_of_month=? order by actual_time,fid";
  private PreparedStatement outputDirectFlightsStatement;

  private static final String OUTPUT_INDIRECT_FLIGHTS = "select top(?) f1.day_of_month as m1,f1.carrier_id as cr1,f1.flight_num as n1,f1.origin_city as o1,"
  +"f1.dest_city as d1,f1.actual_time as a1,f1.capacity as c1,"
  +"f1.price as p1,f1.fid as f1,f2.day_of_month as m2,f2.carrier_id as cr2,f2.flight_num as n2,f2.origin_city as o2,f2.dest_city as d2,"
  +"f2.actual_time as a2,f2.capacity as c2,f2.price as p2,f2.fid as f2,f1.actual_time+"
  +"f2.actual_time as ac from Flights as f1,flights as f2 where f1.canceled=0 and f2.canceled=0 and f1.origin_city=? and f2.dest_city = ? and"
  +" f1.dest_city=f2.origin_city and f1.day_of_month=f2.day_of_month and f1.day_of_month=? order by f1.actual_time+f2.actual_time";
  private PreparedStatement outputIndirectFlightsStatement;
 
  private static final String INSERT_ITI_TABLE = "insert into Itineraries values(?,?,?)";
  private PreparedStatement insertItiTableStatement;
 
  private static final String ITINERARIES_INFORMATION = "select * from flights where fid=?";
  private PreparedStatement itinerariesInformationStatement;

  private static final String RESERVATIONS_INFORMATION="select * from reservations where username=?";
  private PreparedStatement reservationIfoStatement;

  private static final String LARGEST_UID = "select count(uid) as count from reservations";
  private PreparedStatement largestUidStatement;

  private static final String INSERT_RESERVATION = "Insert into reservations values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
  private PreparedStatement insertReservationStatement;

  private static final String CHECK_RESERV_EXISTS="select count(*) as count from reservations where uid=? and username=? and ifPaid=0";
  private PreparedStatement checkReservExistsStatement;

  private static final String SAFE_DIRECT ="SELECT TOP (?) day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price,fid,canceled "
  +"from flights where origin_city=? and dest_city=? and day_of_month=? and capacity!=0 order by actual_time,fid";
  private PreparedStatement safeDirectStatement;

  private static final String GET_RESERVATION_INFO ="select * from reservations where uid=?";
  private PreparedStatement getReservationsInfoStatement;

  private static final String GET_USER_INFORMATION ="select * from users where username=?";
  private PreparedStatement getUserInformationStatement;

  private static final String UPDATE_USERBALANCE ="update users set initAmount=? where username=?";
  private PreparedStatement updateUserbalanceStatement;

  private static final String UPDATE_PAY = "update reservations set ifPaid=? where uid=?";
  private PreparedStatement updatePayStatement;

  private static final String CHECK_ITICOUNT_FOR_USERS ="select count(*) as count from reservations where username=?";
  private PreparedStatement checkIticountForUsersStatement;

  private static final String PRINT_RESERVATION ="select * from reservations where username=? order by uid";
  private PreparedStatement printReservationStatement;

  private static final String ITI_IFO ="select * from flights where fid=?";
  private PreparedStatement itiInfoStatement;

  private static final String DELETE_USERS = "DELETE FROM users";
  private PreparedStatement deleteUsersStatement;

  private static final String DELETE_RESERVATION = "DELETE FROM reservations";
  private PreparedStatement deleteReservationsStatement;

  private static final String GET_RESERVATION_TO_CANCEL ="select count(*) as count from reservations where username=? and uid=?";
  private PreparedStatement getReservationsToCancelStatement;

  private static final String GET_ALL_RESERVATIONS = "select * from reservations where uid=? and username=?";
  private PreparedStatement getAllReservationsStatement;

  private static final String UPDATE_USERS_BALANCE = "update users set initAmount=? where username=?";
  private PreparedStatement updateUsersBalanceStatement;

  private static final String DELETE_TUPLE_RESERVATIONS ="delete from reservations where uid=?";
  private PreparedStatement deleteTupleReservationsStatement;

  private static final String CHECK_FID1_BOUGHT_COUNT = "select count(*) as count from reservations where fid1=?";
  private PreparedStatement checkFid1BoughtCountStatement;

  private static final String CHECK_FID2_BOUGHT_COUNT = "select count(*) as count from reservations where fid2=?";
  private PreparedStatement checkFid2BoughtCountStatement;
  // transactions
  private static final String BEGIN_TRANSACTION_SQL = "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;";
  private PreparedStatement beginTransactionStatement;

  private static final String COMMIT_SQL = "COMMIT TRANSACTION";
  private PreparedStatement commitTransactionStatement;

  private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
  private PreparedStatement rollbackTransactionStatement;

  class Flight
  {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    @Override
    public String toString()
    {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId +
              " Number: " + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time +
              " Capacity: " + capacity + " Price: " + price;
    }
  }

  public Query(String configFilename)
  {
    this.configFilename = configFilename;
    this.logedin=false;
    this.iti=0;
    this.allfid = new ArrayList<>(0);
  }

  /* Connection code to SQL Azure.  */
  public void openConnection() throws Exception
  {
    configProps.load(new FileInputStream(configFilename));

    jSQLDriver = configProps.getProperty("flightservice.jdbc_driver");
    jSQLUrl = configProps.getProperty("flightservice.url");
    jSQLUser = configProps.getProperty("flightservice.sqlazure_username");
    jSQLPassword = configProps.getProperty("flightservice.sqlazure_password");

    /* load jdbc drivers */
    Class.forName(jSQLDriver).newInstance();

    /* open connections to the flights database */
    conn = DriverManager.getConnection(jSQLUrl, // database
            jSQLUser, // user
            jSQLPassword); // password

    conn.setAutoCommit(true); //by default automatically commit after each statement

    /* You will also want to appropriately set the transaction's isolation level through:
       conn.setTransactionIsolation(...)
       See Connection class' JavaDoc for details.
    */
  }

  public void closeConnection() throws Exception
  {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created. Do not drop any tables and do not
   * clear the flights table. You should clear any tables you use to store reservations
   * and reset the next reservation ID to be 1.
   */
  public void clearTables ()
  {
    try{
      deleteUsersStatement.clearParameters();
    deleteUsersStatement.executeUpdate();
    deleteReservationsStatement.clearParameters();
    deleteReservationsStatement.executeUpdate();
    }catch(SQLException e) { e.printStackTrace(); } 
  }

  /**
   * prepare all the SQL statements in this method.
   * "preparing" a statement is almost like compiling it.
   * Note that the parameters (with ?) are still not filled in
   */
  public void prepareStatements() throws Exception
  {
    beginTransactionStatement = conn.prepareStatement(BEGIN_TRANSACTION_SQL);
    commitTransactionStatement = conn.prepareStatement(COMMIT_SQL);
    rollbackTransactionStatement = conn.prepareStatement(ROLLBACK_SQL);

    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    findOutUsernameStatement=conn.prepareStatement(FIND_OUT_USERNAME);
    insertUserStatement=conn.prepareStatement(INSERT_USER);
    checkUserExistsStatement=conn.prepareStatement(CHECK_USER_EXISTS);
    countDirectFlightsStatement = conn.prepareStatement(COUNT_DIRECT_FLIGHTS);
    countIndirectFlightsStatement=conn.prepareStatement(COUNT_INDIRECT_FLIGHTS);
    outputDirectFlightsStatement=conn.prepareStatement(OUTPUT_DIRECT_FLIGHTS);
    outputIndirectFlightsStatement=conn.prepareStatement(OUTPUT_INDIRECT_FLIGHTS);
    insertItiTableStatement = conn.prepareStatement(INSERT_ITI_TABLE);
    itinerariesInformationStatement=conn.prepareStatement(ITINERARIES_INFORMATION);
    reservationIfoStatement=conn.prepareStatement(RESERVATIONS_INFORMATION);
    largestUidStatement = conn.prepareStatement(LARGEST_UID);
    insertReservationStatement = conn.prepareStatement(INSERT_RESERVATION);
    checkReservExistsStatement = conn.prepareStatement(CHECK_RESERV_EXISTS);
    safeDirectStatement = conn.prepareStatement(SAFE_DIRECT);
    getReservationsInfoStatement = conn.prepareStatement(GET_RESERVATION_INFO);
    getUserInformationStatement=conn.prepareStatement(GET_USER_INFORMATION);
    updateUserbalanceStatement=conn.prepareStatement(UPDATE_USERBALANCE);
    updatePayStatement=conn.prepareStatement(UPDATE_PAY);
    printReservationStatement=conn.prepareStatement(PRINT_RESERVATION);
    itiInfoStatement=conn.prepareStatement(ITI_IFO);
    checkIticountForUsersStatement=conn.prepareStatement(CHECK_ITICOUNT_FOR_USERS);
    deleteReservationsStatement=conn.prepareStatement(DELETE_RESERVATION);
    deleteUsersStatement=conn.prepareStatement(DELETE_USERS);
    getReservationsToCancelStatement = conn.prepareStatement(GET_RESERVATION_TO_CANCEL);
    getAllReservationsStatement = conn.prepareStatement(GET_ALL_RESERVATIONS);
    updateUsersBalanceStatement = conn.prepareStatement(UPDATE_USERS_BALANCE);
    deleteTupleReservationsStatement = conn.prepareStatement(DELETE_TUPLE_RESERVATIONS);
    checkFid1BoughtCountStatement = conn.prepareStatement(CHECK_FID1_BOUGHT_COUNT);
    checkFid2BoughtCountStatement = conn.prepareStatement(CHECK_FID2_BOUGHT_COUNT);
    /* add here more prepare statements for all the other queries you need */
    /* . . . . . . */
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username
   * @param password
   *
   * @return If someone has already logged in, then return "User already logged in\n"
   * For all other errors, return "Login failed\n".
   *
   * Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password)
  {  
    try{
       String resultName = "";
     String resultPass = "";
     beginTransaction();
       checkUserExistsStatement.clearParameters();
       checkUserExistsStatement.setString(1,username);
       ResultSet results=checkUserExistsStatement.executeQuery();     
       results.next();
       resultName=results.getString("username");
       resultPass=results.getString("password");
      results.close();
     if(resultName.equals(username) && resultPass.equals(password) && logedin==false){
         logedin=true;
         this.usernameLogedIn=username;
         commitTransaction();
         return "Logged in as "+username+"\n";
       }else if(resultName.equals(username) && resultPass.equals(password) && logedin==true){
         rollbackTransaction();
         return "User already logged in\n";
       }else{
         rollbackTransaction();
         return "Login failed\n";
       }
    }catch(SQLException e) { 
      try{
        rollbackTransaction();
        return "User already logged in\n";
      } catch (SQLException cse) {return transaction_login(username, password); }
     }
    
  }

  /**
   * Implement the create user function.
   *
   * @param username new user's username. User names are unique the system.
   * @param password new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer (String username, String password, int initAmount)
  { 
   try{
      
      if(password.length()>20 || username.length()>20 || initAmount<0){
      return "Failed to create user\n";
    }else{
      beginTransaction();
      int ifexistUsername=-1;
      
       
      findOutUsernameStatement.clearParameters();
      findOutUsernameStatement.setString(1, username);
      ResultSet results = findOutUsernameStatement.executeQuery();
      results.next();
     ifexistUsername=results.getInt("count");
      if(ifexistUsername>0){
        rollbackTransaction();
      return "Failed to create user\n";
      }else{       
      insertUserStatement.clearParameters();
      insertUserStatement.setString(1,username);
      insertUserStatement.setString(2,password);
      insertUserStatement.setInt(3,initAmount);
      insertUserStatement.executeUpdate();
     commitTransaction();
    return "Created user "+username+"\n";
      }
    
    }
    }catch (SQLException e) { 
      try{
        rollbackTransaction();
        return "Failed to create user\n";
    }catch (SQLException cse) {return transaction_createCustomer (username, password, initAmount); }
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights
   * and flights with two "hops." Only searches for up to the number of
   * itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight if true, then only search for direct flights, otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n".
   * If an error occurs, then return "Failed to search\n".
   *
   * Otherwise, the sorted itineraries printed in the following format:
   *
   * Itinerary [itinerary number]: [number of flights] flight(s), [total flight time] minutes\n
   * [first flight in itinerary]\n
   * ...
   * [last flight in itinerary]\n
   *
   * Each flight should be printed using the same format as in the {@code Flight} class. Itinerary numbers
   * in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
   
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries)
  {
      return transaction_search_unsafe(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries); 
  }

  /**
   * Same as {@code transaction_search} except that it only performs single hop search and
   * do it in an unsafe manner.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight
   * @param dayOfMonth
   * @param numberOfItineraries
   *
   * @return The search results. Note that this implementation *does not conform* to the format required by
   * {@code transaction_search}.
   */
 private String transaction_search_unsafe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries)
  {
     
     int iti=0;
     this.allfid = new ArrayList<>();
    StringBuffer sb = new StringBuffer();
   if(directFlight){
     
         try
        {
      // one hop itineraries
      safeDirectStatement.clearParameters();
      safeDirectStatement.setInt(1,numberOfItineraries);
      safeDirectStatement.setString(2,originCity);
      safeDirectStatement.setString(3,destinationCity);
      safeDirectStatement.setInt(4,dayOfMonth);
      
      ResultSet oneHopResults = safeDirectStatement.executeQuery();
      //int iti=0;
      while (oneHopResults.next())
      { 
        int result_canceled = oneHopResults.getInt("canceled");
      if(result_canceled!=1){
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_fid = oneHopResults.getInt("fid");
        int result_price = oneHopResults.getInt("price");
        String result_carrierId=oneHopResults.getString("carrier_id");
        int result_flightNum=oneHopResults.getInt("flight_num");
        int result_capacity=oneHopResults.getInt("capacity");
        //insertItiTableStatement.clearParameters();
       // insertItiTableStatement.setInt(1,iti);
       // insertItiTableStatement.setInt(2,result_fid);
       // insertItiTableStatement.setInt(3,-1);
       // insertItiTableStatement.executeUpdate();
        allfid.add(result_fid);
        allfid.add(0);
        sb.append("Itinerary "+iti+": 1 flight(s), "+result_time+" minutes\n"+"ID: " + result_fid +
        " Day: "+result_dayOfMonth+ " Carrier: " + result_carrierId + " Number: " + result_flightNum + " Origin: " + result_originCity + 
        " Dest: " + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
      iti++;
      }
      }

      oneHopResults.close();
    } catch (SQLException e) { e.printStackTrace(); }
    
   return sb.toString();
   }else{
     int result_countDirect=0;
     int result_countIndirect=0;
     try{
       countDirectFlightsStatement.clearParameters();
       countDirectFlightsStatement.setString(1,originCity);
       countDirectFlightsStatement.setString(2,destinationCity);
       countDirectFlightsStatement.setInt(3,dayOfMonth);
      ResultSet results = countDirectFlightsStatement.executeQuery();
       results.next();
        result_countDirect=results.getInt("count");
      countIndirectFlightsStatement.clearParameters();
      countIndirectFlightsStatement.setString(1,originCity);
      countIndirectFlightsStatement.setString(2,destinationCity);
      countIndirectFlightsStatement.setInt(3,dayOfMonth);
      ResultSet resultIn = countIndirectFlightsStatement.executeQuery();
      resultIn.next();
      result_countIndirect=resultIn.getInt("count");
   
     }catch (SQLException e) { e.printStackTrace(); }
     int DirectIti=0;
     int IndirectIti=0;
     if(result_countDirect>=numberOfItineraries){
        DirectIti=numberOfItineraries;
     }else if((result_countDirect+result_countIndirect)<numberOfItineraries){
       DirectIti=result_countDirect;
       IndirectIti=result_countIndirect;
     }else{
       DirectIti=result_countDirect;
       IndirectIti=numberOfItineraries-result_countDirect;
     }
   
     try{
       outputDirectFlightsStatement.clearParameters();
       outputDirectFlightsStatement.setInt(1,DirectIti);
       outputDirectFlightsStatement.setString(2,originCity);
       outputDirectFlightsStatement.setString(3,destinationCity);
       outputDirectFlightsStatement.setInt(4,dayOfMonth);
       ResultSet DiResult = outputDirectFlightsStatement.executeQuery();
       DiResult.next();
       outputIndirectFlightsStatement.clearParameters();
       outputIndirectFlightsStatement.setInt(1,DirectIti);
       outputIndirectFlightsStatement.setString(2,originCity);
       outputIndirectFlightsStatement.setString(3,destinationCity);
       outputIndirectFlightsStatement.setInt(4,dayOfMonth);
       ResultSet IndiResult=outputIndirectFlightsStatement.executeQuery();
       IndiResult.next();
       
       for(int i=0;i<numberOfItineraries;i++){
         if(DirectIti>0 && IndirectIti >0){
           if(DiResult.getInt("actual_time")<=IndiResult.getInt("ac")){
              int result_dayOfMonth = DiResult.getInt("day_of_month");
              String result_originCity = DiResult.getString("origin_city");
              String result_destCity = DiResult.getString("dest_city");
              int result_time = DiResult.getInt("actual_time");
              int result_fid = DiResult.getInt("fid");
              int result_price = DiResult.getInt("price");
              String result_carrierId=DiResult.getString("carrier_id");
              int result_flightNum=DiResult.getInt("flight_num");
               int result_capacity=DiResult.getInt("capacity");

              //  insertItiTableStatement.clearParameters();
              // insertItiTableStatement.setInt(1,iti);
              // insertItiTableStatement.setInt(2,result_fid);
              // insertItiTableStatement.setInt(3,-1);
              // insertItiTableStatement.executeUpdate();
              allfid.add(result_fid);
              allfid.add(0);
              sb.append("Itinerary "+iti+": 1 flight(s), "+result_time+" minutes\n"+"ID: " + result_fid + " Day: "+result_dayOfMonth+ 
              " Carrier: " + result_carrierId + " Number: " + result_flightNum + " Origin: " + result_originCity + " Dest: " + result_destCity + 
              " Duration: " + result_time + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
               DirectIti--;
               DiResult.next();
               iti++;
            }else{
              int m1 = IndiResult.getInt("m1");
              String o1 =IndiResult.getString("o1");
              String d1 = IndiResult.getString("d1");
              int a1 = IndiResult.getInt("a1");
              int f1 = IndiResult.getInt("f1");
              int p1 = IndiResult.getInt("p1");
              String cr1=IndiResult.getString("cr1");
              int n1=IndiResult.getInt("n1");
               int c1=IndiResult.getInt("c1");

               int m2 = IndiResult.getInt("m2");
              String o2 = IndiResult.getString("o2");
              String d2 = IndiResult.getString("d2");
              int a2 = IndiResult.getInt("a2");
              int f2 = IndiResult.getInt("f2");
              int p2 = IndiResult.getInt("p2");
              String cr2=IndiResult.getString("cr2");
              int n2=IndiResult.getInt("n2");
               int c2=IndiResult.getInt("c2");
               int ac=IndiResult.getInt("ac");
               allfid.add(f1);
               allfid.add(f2);
               if(f1<f2){
                 sb.append("Itinerary "+iti+": 2 flight(s), "+ac+" minutes\n"+"ID: " + f1 + " Day: "+m1+" Carrier: " + cr1 + 
                 " Number: " + n1 + " Origin: " + o1 + " Dest: " + d1 + " Duration: " + a1 + " Capacity: " + c1 + " Price: " + p1 + "\n"+"ID: " + f2 + 
                 " Day: "+m2+" Carrier: " + cr2 + " Number: " + n2 + " Origin: " + o2 + " Dest: " + d2 + " Duration: " + a2 + " Capacity: " + c2 + " Price: " 
                 + p2 + "\n");
               }else{
                sb.append("Itinerary "+iti+": 2 flight(s), "+ac+" minutes\n"+"ID: " + f2 + " Day: "+m2+" Carrier: " + cr2 + " Number: " + n2 + 
                " Origin: " + o2 + " Dest: " + d2 + " Duration: " + a2 + " Capacity: " + c2 + " Price: " + p2 + "\n"+"ID: " + f1 + " Day: "+m1+
                " Carrier: " + cr1 + " Number: " + n1 + " Origin: " + o1 + " Dest: " + d1 + " Duration: " + a1 + " Capacity: " + c1 + " Price: " + p1 + "\n");
               }
              // insertItiTableStatement.clearParameters();
              // insertItiTableStatement.setInt(1,iti);
              // insertItiTableStatement.setInt(2,f1);
              // insertItiTableStatement.setInt(3,f2);
              // insertItiTableStatement.executeUpdate();
               IndirectIti--;
               IndiResult.next();
               iti++;
            }
         }else if(DirectIti<=0){
              int m1 = IndiResult.getInt("m1");
              String o1 =IndiResult.getString("o1");
              String d1 = IndiResult.getString("d1");
              int a1 = IndiResult.getInt("a1");
              int f1 = IndiResult.getInt("f1");
              int p1 = IndiResult.getInt("p1");
              String cr1=IndiResult.getString("cr1");
              int n1=IndiResult.getInt("n1");
               int c1=IndiResult.getInt("c1");

               int m2 = IndiResult.getInt("m2");
              String o2 = IndiResult.getString("o2");
              String d2 = IndiResult.getString("d2");
              int a2 = IndiResult.getInt("a2");
              int f2 = IndiResult.getInt("f2");
              int p2 = IndiResult.getInt("p2");
              String cr2=IndiResult.getString("cr2");
              int n2=IndiResult.getInt("n2");
               int c2=IndiResult.getInt("c2");
               int ac=IndiResult.getInt("ac");
               allfid.add(f1);
               allfid.add(f2);
               if(f1<f2){
                 sb.append("Itinerary "+iti+": 2 flight(s), "+ac+" minutes\n"+"ID: " + f1 + " Day: "+m1+" Carrier: " + cr1 + " Number: " + n1 + 
                 " Origin: " + o1 + " Dest: " + d1 + " Duration: " + a1 + " Capacity: " + c1 + " Price: " + p1 + "\n"+"ID: " + f2 + " Day: "+m2+
                 " Carrier: " + cr2 + " Number: " + n2 + " Origin: " + o2 + " Dest: " + d2 + " Duration: " + a2 + " Capacity: " + c2 + " Price: " + p2 + "\n");
               }else{
                sb.append("Itinerary "+iti+": 2 flight(s), "+ac+" minutes\n"+"ID: " + f2 + " Day: "+m2+" Carrier: " + cr2 + " Number: " + n2 + 
                " Origin: " + o2 + " Dest: " + d2 + " Duration: " + a2 + " Capacity: " + c2 + " Price: " + p2 + "\n"+"ID: " + f1 + " Day: "+m1+" Carrier: " + 
                cr1 + " Number: " + n1 + " Origin: " + o1 + " Dest: " + d1 + " Duration: " + a1 + " Capacity: " + c1 + " Price: " + p1 + "\n");
               }
             //  insertItiTableStatement.clearParameters();
              // insertItiTableStatement.setInt(1,iti);
              // insertItiTableStatement.setInt(2,f1);
              // insertItiTableStatement.setInt(3,f2);
              // insertItiTableStatement.executeUpdate();
               IndirectIti--;
               IndiResult.next();
               iti++;
         }else{
           int result_dayOfMonth = DiResult.getInt("day_of_month");
              String result_originCity = DiResult.getString("origin_city");
              String result_destCity = DiResult.getString("dest_city");
              int result_time = DiResult.getInt("actual_time");
              int result_fid = DiResult.getInt("fid");
              int result_price = DiResult.getInt("price");
              String result_carrierId=DiResult.getString("carrier_id");
              int result_flightNum=DiResult.getInt("flight_num");
               int result_capacity=DiResult.getInt("capacity");
              // insertItiTableStatement.clearParameters();
              // insertItiTableStatement.setInt(1,iti);
              // insertItiTableStatement.setInt(2,result_fid);
              // insertItiTableStatement.setInt(3,-1);
              // insertItiTableStatement.executeUpdate();
              allfid.add(result_fid);
              allfid.add(0);
              sb.append("Itinerary "+iti+": 1 flight(s), "+result_time+" minutes\n"+"ID: " + result_fid + " Day: "+result_dayOfMonth+
              " Carrier: " + result_carrierId + " Number: " + result_flightNum + " Origin: " + result_originCity + " Dest: " + result_destCity + " Duration: " + 
              result_time + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
               DirectIti--;
               DiResult.next();
               iti++;
         }

        }

      }catch (SQLException e) { e.printStackTrace(); }
        
         return sb.toString();
    }

    
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   * If try to book an itinerary with invalid ID, then return "No such itinerary {@code itineraryId}\n".
   * If the user already has a reservation on the same day as the one that they are trying to book now, then return
   * "You cannot book two flights in the same day\n".
   * For all other errors, return "Booking failed\n".
   *
   * And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n" where
   * reservationId is a unique number in the reservation system that starts from 1 and increments by 1 each time a
   * successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId)
  {
     try{
       if(!logedin){
      return "Cannot book reservations, not logged in\n";
    }else if(itineraryId>((allfid.size()-2)/2) || itineraryId<0){
      return "No such itinerary "+itineraryId+"\n";
    }else if(allfid.size()!=0){
       int fidindex1=itineraryId*2;
       int fidindex2=itineraryId*2+1;
       int fid1=allfid.get(fidindex1);
       int fid2=allfid.get(fidindex2);
       int fid1Day=-1;
       int fid2Day=-1;
       int uid=0;
       int fid1Price=-1;
       int fid2Price=-1;
       String fid1Carrier="0";
       String fid2Carrier="0";
       int fid1Num=-1;
       int fid2Num=-1;
       String fid1Origin="0";
       String fid2Origin="0";
       String fid1Dest="0";
       String fid2Dest="0";
       int fid1Duration=-1;
       int fid2Duration=-1;
       int fid1Capacity=-1;
       int fid2Capacity=-1;
       beginTransaction();
         if(fid1!=0){
         itinerariesInformationStatement.clearParameters();
         itinerariesInformationStatement.setInt(1,fid1);
         ResultSet fid1Result = itinerariesInformationStatement.executeQuery();
       // since fid is primary key in flights, so there is only one
         fid1Result.next();
         fid1Day=fid1Result.getInt("day_of_month");
         fid1Price=fid1Result.getInt("price");
         fid1Carrier=fid1Result.getString("carrier_id");
         fid1Num=fid1Result.getInt("flight_num");
         fid1Origin=fid1Result.getString("origin_city");
         fid1Dest=fid1Result.getString("dest_city");
         fid1Duration=fid1Result.getInt("actual_time");
         fid1Capacity=fid1Result.getInt("capacity");

         fid1Result.close();
       }else{
          fid1Day=0;
          fid2Price=0;
          fid1Carrier="0";
         fid1Num=0;
         fid1Origin="0";
         fid1Dest="0";
         fid1Duration=0;
         fid1Capacity=0;
      // there is no "0" date in a month, so if fid=0, the day of that flight is 0, means the flight is not exist
       }
       if(fid2!=0){
          itinerariesInformationStatement.clearParameters();
          itinerariesInformationStatement.setInt(1,fid2);
          ResultSet fid2Result = itinerariesInformationStatement.executeQuery();
         // since fid is primary key in flights, so there is only one
          fid2Result.next();
         fid2Day=fid2Result.getInt("day_of_month");
         fid2Price=fid2Result.getInt("price");
        fid2Carrier=fid2Result.getString("carrier_id");
         fid2Num=fid2Result.getInt("flight_num");
         fid2Origin=fid2Result.getString("origin_city");
         fid2Dest=fid2Result.getString("dest_city");
         fid2Duration=fid2Result.getInt("actual_time");
         fid2Capacity=fid2Result.getInt("capacity");
         fid2Result.close();
       }else{
          fid2Day=0;
          fid2Price=0;
           fid2Carrier="0";
         fid2Num=0;
         fid2Origin="0";
         fid2Dest="0";
         fid2Duration=0;
         fid2Capacity=0;
       }
       
         reservationIfoStatement.clearParameters();
         reservationIfoStatement.setString(1,usernameLogedIn);
         ResultSet userResult=reservationIfoStatement.executeQuery();
         while(userResult.next()){
           int bookedData=userResult.getInt("day_of_month");
           if(bookedData==fid1Day || bookedData==fid2Day){
             rollbackTransaction();
             return "You can not book flights in the same day\n";
           }
         }
         checkFid1BoughtCountStatement.clearParameters();
         checkFid1BoughtCountStatement.setInt(1,fid1);
         ResultSet fid1count=checkFid1BoughtCountStatement.executeQuery();
         fid1count.next();
         int countfid1 = fid1count.getInt("count");
         fid1count.close();

         checkFid2BoughtCountStatement.clearParameters();
         checkFid2BoughtCountStatement.setInt(1,fid2);
         ResultSet fid2count=checkFid2BoughtCountStatement.executeQuery();
         fid2count.next();
         int countfid2 = fid2count.getInt("count");
         fid2count.close();

         if(fid1!=0){
           if(countfid1>=fid1Capacity){
             rollbackTransaction();
             return "Booking failed\n";
           }
         }
         if(fid2!=0){
           if(countfid2>=fid2Capacity){
             rollbackTransaction();
             return "Booking failed\n";
           }
         }


         userResult.close();
           largestUidStatement.clearParameters();
         ResultSet largestUID = largestUidStatement.executeQuery();
         
         largestUID.next();
          uid=largestUID.getInt("count");
         largestUID.close();
         uid=uid+1;
         insertReservationStatement.clearParameters();
         insertReservationStatement.setInt(1,uid);
         insertReservationStatement.setString(2,usernameLogedIn);
         insertReservationStatement.setInt(3,fid1);
         insertReservationStatement.setInt(4,fid2);
         insertReservationStatement.setInt(5,fid1Day);
         insertReservationStatement.setInt(6,0);
         insertReservationStatement.setInt(7,fid1Price);
         insertReservationStatement.setInt(8,fid2Price);
         insertReservationStatement.setInt(9,fid1Price+fid2Price);
         insertReservationStatement.setString(10,fid1Carrier);
         insertReservationStatement.setString(11,fid2Carrier);
         insertReservationStatement.setInt(12,fid1Num);
          insertReservationStatement.setInt(13,fid2Num);
           insertReservationStatement.setString(14,fid1Origin);
           insertReservationStatement.setString(15,fid2Origin);
           insertReservationStatement.setString(16,fid1Dest);
           insertReservationStatement.setString(17,fid2Dest);
         insertReservationStatement.setInt(18,fid1Duration);
          insertReservationStatement.setInt(19,fid2Duration);
          insertReservationStatement.setInt(20,fid1Capacity);
          insertReservationStatement.setInt(21,fid2Capacity);
         insertReservationStatement.executeUpdate();
         commitTransaction();
       return "Booked flight(s), reservation ID: " + uid + "\n";

    }else{
      rollbackTransaction();
      return "Booking failed\n";
    }
    
     }catch (SQLException e) {
       try{
         rollbackTransaction();
         return "Booking failed\n";
       }catch (SQLException cse) {transaction_book(itineraryId); }
     }
     
     return "Booking failed\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n"
   * If the user has no reservations, then return "No reservations found\n"
   * For all other errors, return "Failed to retrieve reservations\n"
   *
   * Otherwise return the reservations in the following format:
   *
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * ...
   *
   * Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations()
  { 
    try{
      int iticount=-1;
    if(!logedin){
      return "Cannot view reservations, not logged in\n";
    }else{
       beginTransaction();
       checkIticountForUsersStatement.clearParameters();
       checkIticountForUsersStatement.setString(1,usernameLogedIn);
       ResultSet count=checkIticountForUsersStatement.executeQuery();
       count.next();
       iticount=count.getInt("count");
       count.close();
     
      if(iticount<1){
        rollbackTransaction();
        return "No reservations found \n";
      }else{
        StringBuffer sb = new StringBuffer();
        int iti=0;
        
          printReservationStatement.clearParameters();
          printReservationStatement.setString(1,usernameLogedIn);
          ResultSet result=printReservationStatement.executeQuery();
          while(result.next()){
            boolean paid = false;
           int result_uid=result.getInt("uid");
           int result_fid1=result.getInt("fid1");
          int  result_fid2=result.getInt("fid2");
           int result_day=result.getInt("day_of_month");
           int result_ifpaid=result.getInt("ifPaid");
           int result_fid1price=result.getInt("fid1Price");
           int result_fid2price=result.getInt("fid2Price");
            if(result_ifpaid==1){
              paid=true;
            }
            String result_Carrier1=result.getString("fid1Carrier");
            String result_Carrier2=result.getString("fid2Carrier");
            int result_Num1=result.getInt("fid1Num");
            int result_Num2=result.getInt("fid2Num");
            String result_Origin1=result.getString("fid1Origin");
            String result_Origin2=result.getString("fid2Origin");
            String result_Dest1=result.getString("fid1Dest");
            String result_Dest2=result.getString("fid2Dest");
            int result_Duration1=result.getInt("fid1Duration");
            int result_Duration2=result.getInt("fid2Duration");
            int result_Capacity1=result.getInt("fid1Capacity");
            int result_Capacity2=result.getInt("fid2Capacity");
            if(result_fid2==0){
              sb.append("Reservation "+result_uid+" paid: "+paid+":\n"+"ID: " + result_fid1 + " Day: "+result_day+" Carrier: " + result_Carrier1 + 
              " Number: " + result_Num1 + " Origin: " + result_Origin1 + " Dest: " + result_Dest1 + " Duration: " + result_Duration1 + " Capacity: " + 
              result_Capacity1 + " Price: " + result_fid1price + "\n");
            }else if(result_fid1<=result_fid2){
              sb.append("Reservation "+result_uid+" paid: "+paid+":\n"+"ID: " + result_fid1 + " Day: "+result_day+" Carrier: " + result_Carrier1 + 
              " Number: " + result_Num1 + " Origin: " + result_Origin1 + " Dest: " + result_Dest1 + " Duration: " + result_Duration1 + " Capacity: " + 
              result_Capacity1 + " Price: " + result_fid1price + "\n"+"ID: " + result_fid2 + " Day: "+result_day+" Carrier: " + result_Carrier2 + " Number: " + 
              result_Num2 + " Origin: " + result_Origin2 + " Destination: " + result_Dest2 + " Duration: " + result_Duration2 + " Capacity: " + 
              result_Capacity2 + " Price: " + result_fid2price + "\n");
            }else{
              sb.append("Reservation "+result_uid+" paid: "+paid+":\n"+"ID: " + result_fid2 + " Day: "+result_day+" Carrier: " 
              + result_Carrier2 + " Number: " + result_Num2 + " Origin: " + result_Origin2 + " Dest: " + result_Dest2 + " Duration: " + result_Duration2 + 
              " Capacity: " + result_Capacity2 + " Price: " + result_fid2price + "\n"+"ID: " + result_fid1 + " Day: "+result_day+" Carrier: " + 
              result_Carrier1 + " Number: " + result_Num1 + " Origin: " + result_Origin1 + " Destination: " + result_Dest1 + " Duration: " + 
              result_Duration1 + " Capacity: " + result_Capacity1 + " Price: " + result_fid1price + "\n");
            }     
          }
          result.close();
        commitTransaction();
        return sb.toString();
      }
    }
    }catch (SQLException e) {
      try{
         rollbackTransaction();
         return "Failed to retrieve reservations\n";
      }catch (SQLException cse) {return transaction_reservations(); }
    }
    
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n"
   * For all other errors, return "Failed to cancel reservation [reservationId]"
   *
   * If successful, return "Canceled reservation [reservationId]"
   *
   * Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId)
  {  
    try{
      if(!logedin){
        return "Cannot cancel reservations, not logged in\n";
      }else{
         beginTransaction();
         getReservationsToCancelStatement.clearParameters();
         getReservationsToCancelStatement.setString(1,usernameLogedIn);
         getReservationsToCancelStatement.setInt(2,reservationId);
         ResultSet counts=getReservationsToCancelStatement.executeQuery();
         counts.next();
         int count =counts.getInt("count");
         if(count<1){
           rollbackTransaction();
           return "Failed to cancel reservation "+reservationId+"\n";
         }else{
           getAllReservationsStatement.clearParameters();
           getAllReservationsStatement.setInt(1,reservationId);
           getAllReservationsStatement.setString(2,usernameLogedIn);
           ResultSet results = getAllReservationsStatement.executeQuery();
           results.next();
           int paidOrNot = results.getInt("ifPaid");
           int paidPrice = results.getInt("totalprice");
           if(paidOrNot==1){
             getUserInformationStatement.clearParameters();
             getUserInformationStatement.setString(1,usernameLogedIn);
             ResultSet theresult = getUserInformationStatement.executeQuery();
             theresult.next();
             int moneyleft =theresult.getInt("initAmount");
             moneyleft = moneyleft+paidPrice;
             updateUsersBalanceStatement.clearParameters();
             updateUsersBalanceStatement.setInt(1,moneyleft);
             updateUsersBalanceStatement.setString(2,usernameLogedIn);
             updateUsersBalanceStatement.executeUpdate();
             }
             deleteTupleReservationsStatement.clearParameters();
             deleteTupleReservationsStatement.setInt(1,reservationId);
             deleteTupleReservationsStatement.executeUpdate();
             commitTransaction();
            return "Canceled reservation "+reservationId+"\n";
         }
      }

    }catch (SQLException e) { 
      try{
        rollbackTransaction();
        return "Failed to cancel reservation "+reservationId+"\n";
      }catch (SQLException cse) { transaction_cancel(reservationId); }
     }
    // only implement this if you are interested in earning extra credit for the HW!
    return "Failed to cancel reservation " + reservationId+"\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   * If the reservation is not found / not under the logged in user's name, then return
   * "Cannot find unpaid reservation [reservationId] under user: [username]\n"
   * If the user does not have enough money in their account, then return
   * "User has only [balance] in account but itinerary costs [cost]\n"
   * For all other errors, return "Failed to pay for reservation [reservationId]\n"
   *
   * If successful, return "Paid reservation: [reservationId] remaining balance: [balance]\n"
   * where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay (int reservationId)
  { 
    try{
       int ifexist=-1;
    if(!logedin){
      return "Cannot pay, not logged in\n";
    }else{
      beginTransaction();
      checkReservExistsStatement.clearParameters();
      checkReservExistsStatement.setInt(1,reservationId);
      checkReservExistsStatement.setString(2,usernameLogedIn);
      ResultSet exist = checkReservExistsStatement.executeQuery();
      exist.next();
      ifexist=exist.getInt("count");
      if(ifexist!=1){
        rollbackTransaction();
         return "Cannot find unpaid reservation "+reservationId+" under user: "+usernameLogedIn+"\n";
      }else{
        int haveprice=-1;
        int totalprice=-1;
           getReservationsInfoStatement.clearParameters();
           getReservationsInfoStatement.setInt(1,reservationId);
           ResultSet itiInfo =getReservationsInfoStatement.executeQuery();
           itiInfo.next();
           totalprice=itiInfo.getInt("totalprice");
           itiInfo.close();
           getUserInformationStatement.clearParameters();
           getUserInformationStatement.setString(1,usernameLogedIn);
           ResultSet userInfo = getUserInformationStatement.executeQuery();
           userInfo.next();
           haveprice = userInfo.getInt("initAmount");
           itiInfo.close();
         if(haveprice<totalprice){
           return "User has only "+haveprice+" in account butitinerary costs "+totalprice+"\n";
         }else{
           int remainBalance=haveprice-totalprice;
             updateUserbalanceStatement.clearParameters();
             updateUserbalanceStatement.setInt(1,remainBalance);
             updateUserbalanceStatement.setString(2,usernameLogedIn);
             updateUserbalanceStatement.executeUpdate();
             updatePayStatement.clearParameters();
             updatePayStatement.setInt(1,1);
             updatePayStatement.setInt(2,reservationId);
             updatePayStatement.executeUpdate();
             commitTransaction();
             return "Paid reservation: "+reservationId+" remaining balance: "+remainBalance+"\n";
         }
      }
    }
    }catch (SQLException e) {
      try{
        rollbackTransaction();
         return "Failed to pay for reservation " + reservationId + "\n";
      }catch (SQLException cse) {return transaction_pay (reservationId);}
    }
      
  }

  /* some utility functions below */

  public void beginTransaction() throws SQLException
  {
    conn.setAutoCommit(false);
    beginTransactionStatement.executeUpdate();
  }

  public void commitTransaction() throws SQLException
  {
    commitTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  public void rollbackTransaction() throws SQLException
  {
    rollbackTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  /**
   * Shows an example of using PreparedStatements after setting arguments. You don't need to
   * use this method if you don't want to.
   */
  private int checkFlightCapacity(int fid) throws SQLException
  {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }


}
