	/* Purpose: This demo shows using objects, SQL, and native access side-by-side in a Java application, 
	* connecting to InterSystems IRIS.
	*
	* To Test: Run to see objects and SQL working side-by-side. Then uncomment the line to execute StoreAirfare to see
	* creating a custom data structure using the Native API.	
	*/
	import java.sql.ResultSet;
	import java.sql.SQLException;
	import java.sql.Statement;
	import java.util.ArrayList;
	import java.util.Scanner;

	import com.intersystems.jdbc.IRIS;
	import com.intersystems.xep.Event;
	import com.intersystems.xep.EventPersister;
	import com.intersystems.xep.PersisterFactory;
	import com.intersystems.jdbc.IRISConnection;


	//Purpose: This class shows a multimodel example in ObjectScript. SQL is used to display airports,
	//objects are stored directly, and a custom data structure is used to determine airfare.
	public class multimodelQS {

		public static void main(String[] args) {
			String user = "SuperUser";
			String pass = "SYS";
			String server = "localhost";
			int port = 51773;
			
			try {
				//Connect to database using EventPersister, which is based on IRISDataSource
		        EventPersister xepPersister = PersisterFactory.createPersister();
		        xepPersister.connect(server,port,"User",user,pass); 
		        System.out.println("Connected to InterSystems IRIS via JDBC.");
		        xepPersister.deleteExtent("Demo.Airport");   // remove old test data
		        xepPersister.importSchema("Demo.Airport");   // import flat schema
		        xepPersister.deleteExtent("Demo.Location");   // remove old test data
		        xepPersister.importSchema("Demo.Location");   // import flat schema
		       
		        //***Initializations***
		        //Create XEP Event for object access
		        Event xepEvent = xepPersister.getEvent("Demo.Airport");

		        //Create JDBC statement object for SQL and IRIS Native access
		        Statement myStatement = xepPersister.createStatement();
		        
		        //Create IRIS Native object
		        IRIS irisNative = IRIS.createIRIS((IRISConnection)xepPersister);
		        
		        
		        //***Running code***
		        System.out.println("Generating stock info table...");
				
		        // Populate 5 airport objects and save to the database using XEP
				populateAirports(xepEvent);
				
				// Get all airports using JDBC
				getAirports(myStatement);
				
				// Store natively - Uncomment the following line for task 3
				// StoreAirfare(irisNative);
					
				//Close everything
			    xepEvent.close();
			    xepPersister.close();
							
			} catch (SQLException e) {
				 System.out.println("Error creating stock listing: " + e.getMessage());
			}
		        
		}
		
		//Store objects directly to InterSystems IRIS
		public static void populateAirports(Event xepEvent) {
			ArrayList<Demo.Airport> airportList = new ArrayList<Demo.Airport>();
			
			//1. Boston
			Demo.Airport newAirport = new Demo.Airport();
			newAirport.name = "Boston Logan International";
			newAirport.code = "BOS";
			newAirport.location = new Demo.Location();
			newAirport.location.city = "Boston";
			newAirport.location.state = "MA";
			airportList.add(newAirport);
			
			//2. Philadephia
			Demo.Airport newAirport2 = new Demo.Airport();
			newAirport2.name =  "Philadephia International";
			newAirport2.code = "PHL";
			newAirport2.location = new Demo.Location();
			newAirport2.location.city = "Philadelphia";
			newAirport2.location.state = "PA";
			airportList.add(newAirport2);
			
			//3. Austin
			Demo.Airport newAirport3 = new Demo.Airport();
			newAirport3.name = "Austin–Bergstrom International";
			newAirport3.code = "AUS";
			newAirport3.location = new Demo.Location();
			newAirport3.location.city = "Austin";
			newAirport3.location.state = "TX";
			airportList.add(newAirport3);
			
			//4. San Francisco
			Demo.Airport newAirport4 = new Demo.Airport();
			newAirport4.name = "San Francisco International";
			newAirport4.code = "SFO";
			newAirport4.location = new Demo.Location();
			newAirport4.location.city = "San Francisco";
			newAirport4.location.state = "CA";
			airportList.add(newAirport4);
			
			//5. O'hare
			Demo.Airport newAirport5 = new Demo.Airport();
			newAirport5.name = "O'hare International";
			newAirport5.code = "ORD";
			newAirport5.location = new Demo.Location();
			newAirport5.location.city = "Chicago";
			newAirport5.location.state = "IL";
			airportList.add(newAirport5);
			
			Demo.Airport[] airportArray = airportList.toArray(new Demo.Airport[airportList.size()]);
			xepEvent.store(airportArray);
			System.out.println("Stored 5 airports");
		}
		
		///Display all airports using JDBC
		public static void getAirports(Statement myStatement)
		{
			ResultSet myRS;
			try {
				myRS = myStatement.executeQuery("SELECT name,code,location FROM demo.airport");
			
				System.out.println("Name\t\t\t\t\tCode\t\tCity");
				while(myRS.next())
				{
					System.out.println(myRS.getString("name")+"\t\tcode: "+myRS.getString("code")+"\tcity: ");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		///Create a custom data structure to store airfare in a graph-like structure and retrieve airfare based on nodes
		///Takes departure airport and arrival airport as arguments
		public static void StoreAirfare(IRIS irisNative)
		{		
			//Store routes and distance between airports 
			//This API sets the value, for a global, with the following keys
			//For example, ^AIRPORT("BOS","AUS") = 1698
			irisNative.set("1698","^AIRPORT", "BOS","AUS");
			irisNative.set("450","^AIRPORT", "BOS","AUS","AA150");
			irisNative.set("550","^AIRPORT", "BOS","AUS","AA290");
			irisNative.set("200","^AIRPORT", "BOS","PHL","UA110");
			irisNative.set("700","^AIRPORT", "BOS","BIS","AA330");
			irisNative.set("710","^AIRPORT", "BOS","BIS","UA208");
			
			//Prompt
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter departure airport: ");
			String fromAirport = scanner.next();
			System.out.print("Enter destination airport: ");
			String toAirport = scanner.next();
			
			//Query for routes based on input
			String hasRoutes = "This path has no routes";
			int isDefined = irisNative.isDefined("^AIRPORT", fromAirport, toAirport);
					
			if (isDefined==11 || isDefined==1 ) { hasRoutes =  "This path has routes"; } 
			System.out.println("");
			System.out.println("Printed to ^AIRPORT global. The distance in miles between "+ fromAirport + " and " + toAirport + 
					" is: " + irisNative.getString("^AIRPORT", fromAirport, toAirport) + ". " + hasRoutes );
			
			scanner.close();
		}
}
