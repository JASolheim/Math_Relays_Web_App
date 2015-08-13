/*
 *  File: HomeServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of doGet & doPost methods that are invoked for
 *  the .../Home URL.  This corresponds to the "home" page encountered when a user
 *  successfully logs in to the system.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.net.* ;
import  java.sql.* ;

@WebServlet  (  name = "HomeServlet",  urlPatterns = {"/Home"}  )

public class HomeServlet extends HttpServlet
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
    // obtain values of HTTP request parameters & HTTP session attributes (if extant) ...
    final String       USERNAME             =  request.getParameter("UserName") ;
    final String       PASSWORD             =  request.getParameter("Password") ;
    final String       COMP_ID_STRING       =  request.getParameter("COMPETITION_ID") ;
    boolean            isFacultySponsor     =  false ;
    final HttpSession  SESSION              =  request.getSession() ;
    SysUser            loginUser            =  (SysUser) SESSION.getAttribute("LOGIN_USER") ;
    Role               loginRole            =  (Role) SESSION.getAttribute("LOGIN_ROLE") ;
    Competition        selectedCompetition  =  (Competition) SESSION.getAttribute("COMPETITION") ;
    School             associatedSchool     =  (School) SESSION.getAttribute("ASSOCIATED_SCHOOL") ;

    // prepare to write HTML output via a PrintWriter ...
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;

    // print the first portion of the HTML webpage ...
    out.println ( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n"
      +  "<title>FHSU Math Relays</title>\n"
      +  "<link rel='stylesheet' type='text/css' href='Math_Relays.css'>\n"
      +  "</head>\n<body>\n<hr>\n"  ) ;

    // if user indicated desire to logout, then do so ...
    if ( (loginUser == null) && ((USERNAME == null) || (PASSWORD == null)) )
      {
      SESSION.invalidate() ;
      String message = "No user is currently logged in." ;
      if ( loginUser != null )
        message = (loginUser.getUserName() + " has logged out.") ;
      out.print (  "<h1>" + message + "</h1>\n<hr>\n</body>\n</html>\n"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if

    Connection         dbConn     =  null ;
    Statement          stmnt      =  null ;
    ResultSet          rs         =  null ;
    ResultSetMetaData  rsmd       =  null ;
    String             sqlString  =  null ;

    try
      {
      // attempt to connect to database ...
      URI dbUri = new URI(System.getenv("DATABASE_URL")) ;
      String username = dbUri.getUserInfo().split(":")[0] ;
      String password = dbUri.getUserInfo().split(":")[1] ;
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() ;
      Class.forName ( "org.postgresql.Driver" ) ;
      dbConn = DriverManager.getConnection ( dbUrl, username, password ) ;
      stmnt = dbConn.createStatement() ;

      // is no user is already logged in, then attempt to log in using given username & password ...
      if ( loginUser == null )
        {
        sqlString = "SELECT * FROM MATH_RELAYS.SysUser "
          +  "WHERE   (UPPER(USERNAME) LIKE '" + USERNAME.toUpperCase() + "') "
          +  "AND     (PASSWORD LIKE '" + PASSWORD + "') " ;
        rs = stmnt.executeQuery( sqlString ) ;
        if ( rs.next() )
          {
          loginUser  =  new SysUser
            (  rs.getInt("ID"),           rs.getInt("Role_ID"),       rs.getString("UserName"),
               rs.getString("Password"),  rs.getString("FirstName"),  rs.getString("LastName"),
               rs.getString("EMail"),     rs.getString("Phone")  ) ;
          if ( rs != null ) { rs.close(); rs = null; }
          SESSION.setAttribute ( "LOGIN_USER", loginUser ) ;
          SESSION.setMaxInactiveInterval ( 960 ) ;
          } // end if found username & password in database SysUser table
        } // end if no loginUser

      // if at this point there still is no loginUser, then login must have failed ...
      if ( loginUser == null )
        {
        SESSION.invalidate() ;
        out.print (  "<h1>Login failed.</h1>\n<hr>\n</body>\n</html>\n"  ) ;
        out.flush() ;
        out.close() ;
        return ; // exit doPost method
        } // end if no loginUser
      assert  ( loginUser != null ) ;

      // -----------------------------------------------------------------------------------
      // ---- if execution reaches this point, then a valid user is currently logged in ----
      // -----------------------------------------------------------------------------------

      // ---- determine the role, Administrator or Faculty Sponsor, of newly logged-in user ----
      if ( loginRole == null )
        {
        sqlString = "SELECT * FROM MATH_RELAYS.ROLE R WHERE (R.ID = " + loginUser.getRoleId() + ") " ;
        rs = stmnt.executeQuery ( sqlString ) ;
        if ( rs.next() )
          {
          loginRole  =  new Role ( rs.getInt("ID"), rs.getString("DESCRIPTION") ) ;
          if ( rs != null ) { rs.close(); rs = null; }
          SESSION.setAttribute ( "LOGIN_ROLE", loginRole ) ;
          } // end if found logged-in user's Role
        } // end if loginRole is null
      assert  ( loginRole != null ) ;
      isFacultySponsor  =  (loginRole.getDescription() .equalsIgnoreCase ("Faculty Sponsor")) ;

      // ---- determine the selected competition of the logged-in user ----
      if ( selectedCompetition == null )
        {
        sqlString = "SELECT * FROM MATH_RELAYS.COMPETITION C WHERE (C.ID = " + COMP_ID_STRING + ") " ;
        rs = stmnt.executeQuery ( sqlString ) ;
        if ( rs.next() )
          {
          selectedCompetition
            =  new Competition ( rs.getInt("ID"), rs.getDate("COMPDATE"), rs.getString("NAME") ) ;
          if ( rs != null ) { rs.close(); rs = null; }
          SESSION.setAttribute ( "COMPETITION", selectedCompetition ) ;
          } // end if found logged-in user's Role
        } // end if selectedCompetition is null
      assert  ( selectedCompetition != null ) ;

      // ---- determine associated school if Faculty Sponsor is logged-in ----
      if (  isFacultySponsor && (associatedSchool == null) )
        {
        sqlString  =  "SELECT * FROM MATH_RELAYS.SPONSOR_SCHOOL SS "
                   +  "INNER JOIN MATH_RELAYS.SCHOOL S "
                   +  "ON (SS.School_ID = S.ID)  "
                   +  "WHERE (SS.Competition_ID = " + selectedCompetition.getID() + ") "
                   +  "AND   (SS.Sponsor_ID = " + loginUser.getID() + ") " ;
        rs = stmnt.executeQuery ( sqlString ) ;
        if ( rs.next() )
          {
          associatedSchool = new School
            (  rs.getInt("ID"),             rs.getString("Name"),   rs.getString("Address"),
               rs.getString("City"),        rs.getString("State"),  rs.getString("PostalCode"),
               rs.getString("WebAddress"),  rs.getString("EMail"),  rs.getString("Phone")  ) ;
          SESSION.setAttribute ( "ASSOCIATED_SCHOOL", associatedSchool ) ;
          if ( rs != null ) { rs.close(); rs = null; }
          } // end if found logged-in user's Role
        } // end if faculty sponsor has no associated school
      if ( isFacultySponsor && (associatedSchool == null) )
        {
        SESSION.invalidate() ;
        out.print ( "<h2>Faculty Sponsor " + (loginUser.getFirstName() + " " + loginUser.getLastName())
         + " is not associated with any school for the " + selectedCompetition.getName() + " competition.</h2>\n<hr>\n" ) ;
        out.print ( "<h2>Please select a different competition when logging in.</h2>\n<hr>\n</body>\n</html>\n" ) ;
        out.flush() ;
        out.close() ;
        return ; // exit doPost method
        } // end if
      assert  ( (! isFacultySponsor) || (associatedSchool != null) ) ;

      // ---- display logged-in user's information and logout button ----
      out.println ( "<p>A user is automatically logged out after 16 minutes of inactivity.</p><hr>" ) ;
      out.println ( "<table style='border-style:none;'><tr><td><table style='border-style:none;'>" ) ;
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
      out.println ( "      <input type='submit' formaction='Logout' value='Logout'>") ;
      out.println ( "    </td>") ;
      out.println ( "  </tr>") ;
      out.println ( "</table></td>" ) ;
      out.println ( "<td><table>" ) ;
      out.println ( "  <tr>") ;
      out.println ( "    <td style='border-style:none;'>") ;
      out.println ( "      <a href='Users_Manual.pdf' target='Users_Manual'>User's Manual</a>") ;
      out.println ( "    </td>") ;
      out.println ( "  </tr>") ;
      if ( (loginRole.getDescription() .equalsIgnoreCase ("Administrator")) )
        {
        out.println ( "  <tr>") ;
        out.println ( "    <td style='border-style:none;'>") ;
        out.println ( "      <a href='Database_Description.pdf' target='Database_Description'>Database Description</a>") ;
        out.println ( "    </td>") ;
        out.println ( "  </tr>") ;
        }
      out.println ( "</table>" ) ;
      out.println ( "</form>" ) ;
      out.println ( "</td></tr></table><hr>" ) ;

      // ---- display action options ----
      out.println ( "<form name='actionOptionsForm' method='POST'>" ) ;
      out.println ( "  <div>" ) ;
      out.println ( "    <fieldset>" ) ;
      out.println ( "      <legend>Select an Action</legend>" ) ;
      out.println ( "      <table style='border-style:none;'>" ) ;
      // -----------------------------------------------------------------
      if ( (loginRole.getDescription() .equalsIgnoreCase ("Administrator")) )
           // && (associatedSchool == null) )
        {
        out.println ( "<tr>" ) ;
        out.println ( "<select name='adminsSchoolID' required>" ) ;
        out.println ( "  <option value=''>Select School</option>" ) ;
        sqlString = "SELECT ID, Name FROM Math_Relays.School " ;
        rs = stmnt.executeQuery( sqlString ) ;
        while ( rs.next() )
          {
          out.println ( "  <option value='" + rs.getInt("ID") + "'>" + rs.getString("Name") + "</option>" ) ;
          } // end if
        if ( rs != null ) { rs.close(); rs = null; }
        out.println ( "</select>" ) ;
        out.println ( "" ) ;
        out.println ( "</tr>" ) ;
        } // end if
      // -----------------------------------------------------------------
      out.println ( "        <tr>" ) ;
      out.println ( "          <td style='border-style:none;'>" ) ;
      out.println ( "            <input type='submit' formaction='RegisterStudents' name='RegisterStudents' value='Register Students'>" ) ;
      out.println ( "          </td>" ) ;
      out.println ( "        </tr>" ) ;
      out.println ( "        <tr>" ) ;
      out.println ( "          <td style='border-style:none;'>" ) ;
      out.println ( "            <input type='submit' formaction='ViewSummary' name='ViewSummary' value='View Summary'>" ) ;
      out.println ( "          </td>" ) ;
      out.println ( "        </tr>" ) ;
      out.println ( "      </table>" ) ;
      out.println ( "    </fieldset>" ) ;
      out.println ( "  </div>" ) ;
      out.println ( "</form>" ) ;
      out.println ( "<hr>" ) ;

      if ( loginRole.getDescription() .equalsIgnoreCase ("Administrator") )
        {
        // ----  Table Maintenance Form ----
        out.println (  "    <form name='tableMaintenanceForm' action='TableMaintenance' method='POST'>"
                    +  "      <div><fieldset><legend>Table Maintenance</legend><table>" ) ;
        // print rows of table names ...
        sqlString = "SELECT table_name FROM information_schema.tables where table_schema like 'math_relays' ORDER BY table_name " ;
        rs = stmnt.executeQuery ( sqlString ) ;
        while ( rs.next() )
          {
          String s = rs.getString("table_name").toUpperCase() ;
          out.print ( "<tr><td><input type='submit' name='Table' value='" + s + "'></td></tr>\n" ) ;
          } // end while
        if ( rs != null ) { rs.close(); rs = null; }
        out.print ( "          </table></fieldset></div></form><hr>\n" ) ;
        } // end if

      } // end try block (try to connect to database, that is)
    catch ( URISyntaxException use )       { use.printStackTrace() ;  }
    catch ( ClassNotFoundException cnfe )  { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )            { sqle.printStackTrace() ; }
    finally
      {
      // print closing tags of the HTML webpage ...
      out.print ( "</body>\n</html>\n" ) ;
      out.flush() ;
      out.close() ;
      try
        {
        if ( rs     != null ) rs.close() ;
        if ( stmnt  != null ) stmnt.close() ;
        if ( dbConn != null ) dbConn.close() ;
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block
    } // end doPost method

  } // end HomeServlet class
