/*
 *  File: ViewSummaryServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of the doGet & doPost methods that are executed
 *  when the URL pattern is ".../ViewSummary".  GET requests are forwarded to
 *  the doPost method.  The doPost method generates the web page that summarizes
 *  the student registrations for a particular high school.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.sql.* ;
import  java.net.* ;
import  java.util.* ;

@WebServlet  (  name = "ViewSummaryServlet",  urlPatterns = {"/ViewSummary"}  )

public class ViewSummaryServlet extends HttpServlet
  {

  @Override
  public synchronized void doGet ( final HttpServletRequest request, final HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    this.doPost ( request, response ) ;
    } // end doGet method

  @Override
  protected synchronized void doPost ( HttpServletRequest request, HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    final String adminsSchoolID  =  request.getParameter("adminsSchoolID") ;

    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;

    final HttpSession  SESSION              =  request.getSession() ;
    final SysUser      loginUser            =  (SysUser) SESSION.getAttribute("LOGIN_USER") ;
    final Role         loginRole            =  (Role) SESSION.getAttribute("LOGIN_ROLE") ;
    final Competition  selectedCompetition  =  (Competition) SESSION.getAttribute("COMPETITION") ;
    School       associatedSchool     =  (School) SESSION.getAttribute("ASSOCIATED_SCHOOL") ;

    if ( loginUser == null )
      {
      SESSION.invalidate() ;
      out.println ( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n"
        +  "<title>FHSU Math Relays</title>\n"
        +  "<link rel='stylesheet' type='text/css' href='Math_Relays.css'>\n"
        +  "</head>\n<body>\n<hr>\n<h1>No user is currently logged in.</h1>\n<hr>\n</body>\n</html>\n"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if no loginUser
    assert  ( loginUser != null ) ;

    // ---- declare database-related variables ----
    Connection         dbConn         =  null ;
    Statement          stmnt          =  null ;
    String             sqlString      =  null ;
    ResultSet          rs             =  null ;
    ResultSetMetaData  rsmd           =  null ;
    PreparedStatement  prepStmnt      =  null ;
    // ----------------------------------------

    try
      {
      // ---- connect to database ----
      URI dbUri = new URI(System.getenv("DATABASE_URL")) ;
      String username = dbUri.getUserInfo().split(":")[0] ;
      String password = dbUri.getUserInfo().split(":")[1] ;
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() ;
      Class.forName ( "org.postgresql.Driver" ) ;
      dbConn = DriverManager.getConnection ( dbUrl, username, password ) ;
      dbConn.setAutoCommit ( false ) ;
      stmnt = dbConn.createStatement() ;

      // ---- obtain event names from database ----
      List<Event> eventList = new ArrayList<Event> () ;
      sqlString  =  "SELECT * FROM Math_Relays.Event WHERE (Competition_ID = " + selectedCompetition.getID() + ") ORDER BY ID " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      while ( rs.next() )
        {
        Event event = new Event ( rs.getInt("ID"), rs.getInt("Competition_ID"), rs.getString("Name"), rs.getInt("MinGrade"), rs.getInt("MaxGrade") ) ;
	    eventList.add(event) ;
        } // end while
      if ( rs != null ) { rs.close(); rs = null; }

      if (    (loginRole.getDescription() .equalsIgnoreCase ("Administrator"))
           && (adminsSchoolID != null) )
        {
        sqlString  =  "SELECT * FROM Math_Relays.School WHERE (ID = " + adminsSchoolID + ") " ;
        rs = stmnt.executeQuery ( sqlString ) ;
        if ( rs.next() )
          {
          associatedSchool = new School
            (  rs.getInt("ID"),             rs.getString("Name"),   rs.getString("Address"),
               rs.getString("City"),        rs.getString("State"),  rs.getString("PostalCode"),
               rs.getString("WebAddress"),  rs.getString("EMail"),  rs.getString("Phone")  ) ;
          SESSION.setAttribute ( "ASSOCIATED_SCHOOL", associatedSchool ) ;
          if ( rs != null ) { rs.close(); rs = null; }
          } // end if
        } // end if
      assert  ( associatedSchool != null ) ;

      prepStmnt = dbConn.prepareStatement ( "SELECT * FROM Math_Relays.Student_Count( ?, ?, ?, ?, ?, ?, ? ) " ) ;
      prepStmnt.setInt ( 1, selectedCompetition.getID() ) ;
      prepStmnt.setInt ( 2, associatedSchool.getID() ) ;

      // ---- generate HTML to be sent to browser ----
      out.println ( "<!DOCTYPE html>" ) ;
      out.println ( "<html>" ) ;
      out.println ( "  <head>" ) ;
      out.println ( "    <meta charset='UTF-8'>" ) ;
      out.println ( "    <title>Math Relays</title>" ) ;
      out.println ( "    <link rel='stylesheet' type='text/css' href='Math_Relays.css'>" ) ;
      out.println ( "  </head>" ) ;
      out.println ( "  <body>" ) ;

      // ---- display logged-in user's information and logout button ----
      out.println ( "<hr><table style='border-style:none;'><tr><td><table style='border-style:none;'>" ) ;
      out.println ( "<tr><th>" + (loginRole.getDescription())
        + "</th><th>School</th><th>Competition</th></tr>" ) ;
      out.println ( "<tr><td>" +  (loginUser.getFirstName() + " " + loginUser.getLastName())
        + "</td><td>" + ((associatedSchool == null) ? ("Not Applicable") : (associatedSchool.getName()))
        + "</td><td>" + (selectedCompetition.getName()) + "</td></tr>" ) ;
      out.println ( "</table></td><td>" ) ;
      out.println ( "<form method='POST'>" ) ;
      out.println ( "<table>" ) ;
      out.println ( "  <tr>") ;
      out.println ( "    <td style='border-style:none;'>") ;
      out.println ( "      <input type='submit' formaction='Home' value='Main Menu'>") ;
      out.println ( "    </td>") ;
      out.println ( "  </tr>") ;
      out.println ( "  <tr>") ;
      out.println ( "    <td style='border-style:none;'>") ;
      out.println ( "      <input type='submit' formaction='Logout' value='Logout'>") ;
      out.println ( "    </td>") ;
      out.println ( "  </tr>") ;
      out.println ( "</table>" ) ;
      out.println ( "</form>" ) ;
      out.println ( "</td></tr></table><hr>" ) ;

      sqlString =  "SELECT      COUNT(*) AS Student_Count "
                +  "FROM        Math_Relays.Student "
                +  "INNER JOIN  Math_Relays.Student_School "
                +  "ON          ( Student.ID = Student_School.Student_ID ) "
                +  "WHERE       ( Student_School.Competition_ID = " + selectedCompetition.getID() + " ) "
                +  "AND         ( Student_School.School_ID = " + associatedSchool.getID() + " ) " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      int studentCount = -1 ;
      if ( rs.next() )
        studentCount = rs.getInt("Student_Count") ;

      // for each grade, for each event, # of Individuals and # of Team Competitors
      out.println ( "<table>" ) ;
      out.println ( "  <caption style='font-weight:bold;font-style:italic;'>" + studentCount + " "
        + associatedSchool.getName() + " Students are Competing in the " + selectedCompetition.getName() + "</caption>" ) ;
      out.println ( "<tr><th rowspan='3' colspan='2'>Number of Competitors</th><th colspan='12'>Event</th></tr>" ) ;
      out.println ( "<tr>" ) ;

      Iterator<Event> eventIter = eventList.iterator() ;
      while ( eventIter.hasNext() )
        {
        Event event = eventIter.next() ;
        out.println ( "<th colspan='2'>" + event.getName() + "</th>" ) ;
        } // end while eventIter hasNext

      out.println ( "</tr>" ) ;
      out.println ( "<tr style='font-size:smaller;'>" ) ;

      for ( int i = 1 ; i <= eventList.size() ; i ++ )
        out.println ( "<th style='width:7%'>Individual</th><th style='width:7%'>Team</th>" ) ;

      out.println ( "</tr>" ) ;
      for ( int grade = 9 ; grade <= 12 ; grade ++ )
        {
        out.println ( "<tr>" ) ;
        if ( grade == 9 )
          out.println ( "<th rowspan='4'>Competing<br>at<br>Grade<br>Level</th>" ) ;
        out.println ( "<th>" + grade + "</th>" ) ;

        eventIter = eventList.iterator() ;
        while ( eventIter.hasNext() )
          {
          Event event = eventIter.next() ;
          prepStmnt.setInt ( 3, event.getID() ) ;
          prepStmnt.setInt ( 4, grade ) ;
          prepStmnt.setInt ( 5, grade ) ;
          // ---------------------------------------------
          prepStmnt.setString ( 6, String.valueOf('I') ) ;
          prepStmnt.setString ( 7, String.valueOf('I') ) ;
          int  I_count = -1 ;
          rs = prepStmnt.executeQuery() ;
          dbConn.commit();
          if ( rs.next() )
            I_count = rs.getInt("student_count") ;
          if ( rs != null )  { rs.close(); rs = null; }
          // ---------------------------------------------
          prepStmnt.setString ( 6, String.valueOf('A') ) ;
          prepStmnt.setString ( 7, String.valueOf('B') ) ;
          int  AB_count = -1 ;
          rs = prepStmnt.executeQuery() ;
          dbConn.commit();
          if ( rs.next() )
            AB_count = rs.getInt("student_count") ;
          if ( rs != null )  { rs.close(); rs = null; }
          // ---------------------------------------------
          out.println ( "<td>" + I_count + "</td>" ) ;
          out.println ( "<td>" + AB_count + "</td>" ) ;
          } // end while eventIter hasNext
        out.println ( "</tr>" ) ;
        } // end for grade
      out.println ( "<tr><th colspan='2'>Totals</th>" ) ;

      eventIter = eventList.iterator() ;
      while ( eventIter.hasNext() )
        {
        Event event = eventIter.next() ;
        prepStmnt.setInt ( 3, event.getID() ) ;
        prepStmnt.setInt ( 4, 9 ) ;
        prepStmnt.setInt ( 5, 12 ) ;
        prepStmnt.setString ( 6, String.valueOf('I') ) ;
        prepStmnt.setString ( 7, String.valueOf('I') ) ;
        int  count = -1 ;
        rs = prepStmnt.executeQuery() ;
        dbConn.commit();
        if ( rs.next() )
          count = rs.getInt("student_count") ;
        if ( rs != null )  { rs.close(); rs = null; }
        out.println ( "<td>" + count + "</td>" ) ;

        prepStmnt.setString ( 6, String.valueOf('A') ) ;
        prepStmnt.setString ( 7, String.valueOf('B') ) ;
        count = -1 ;
        rs = prepStmnt.executeQuery() ;
        dbConn.commit();
        if ( rs.next() )
          count = rs.getInt("student_count") ;
        if ( rs != null )  { rs.close(); rs = null; }
        out.println ( "<td>" + count + "</td>" ) ;
        } // end while
	  
      out.println ( "</tr>" ) ;
      out.println ( "</table>" ) ;

      out.println ( "    <hr>" ) ;
      out.println ( "  </body>" ) ;
      out.println ( "</html>" ) ;
      } // end try block
    catch ( URISyntaxException use )
      { use.printStackTrace() ; }
    catch ( ClassNotFoundException cnfe )
      { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )
      { sqle.printStackTrace() ; }
    finally
      {
      out.flush();
      out.close() ;
      try
        {
        if ( prepStmnt   !=  null )  { prepStmnt.close();   prepStmnt = null;  }
        if ( stmnt   !=  null )  { stmnt.close();   stmnt = null;  }
        if ( dbConn  !=  null )  { dbConn.close();  dbConn = null; }
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block
    } // end doPost method

  } // end ViewSummaryServlet class
