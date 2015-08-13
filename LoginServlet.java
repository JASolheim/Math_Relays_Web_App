/*
 *  File: LoginServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of the doGet & doPost methods that are executed
 *  when the URL pattern is ".../Login".  This corresponds to the "login page",
 *  a web page a system user visits in order to log into the system.
 */

package servlet;

import javax.servlet.annotation.WebServlet ;
import javax.servlet.http.* ;
import java.io.PrintWriter ;
import  java.net.* ;
import  java.sql.* ;

@WebServlet  (  name = "LoginServlet",  urlPatterns = {"/Login"}  )

public class LoginServlet extends HttpServlet
  {

  @Override
  public synchronized void doGet ( final HttpServletRequest request, final HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    this.doPost ( request, response ) ;
    } // end doGet method

  @Override
  protected synchronized void doPost ( final HttpServletRequest request, final HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    request.getSession().invalidate() ;
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;

    out.println
      (  "<!DOCTYPE html>\n"
      +  "<html>\n"
      +  "  <head>\n"
      +  "    <meta charset='UTF-8'>\n"
      +  "    <title>Math Relays</title>\n"
      +  "    <link rel='stylesheet' type='text/css' href='Math_Relays.css'>\n"
      +  "  </head>\n  <body><hr>" ) ;

    final String originatingProtocol = request.getHeader("X-Forwarded-Proto") ;
    if ( ! (originatingProtocol.equals("https")) )
      {
      out.println
        (  "<h1>URI scheme must be http<span style='color:red;'>s</span>.</h1>"
        +  "<h1>Example:  http<span style='color:red;'>s</span>://fhsu-math-relays.herokuapp.com/Login</h1>"
        +  "<hr></body></html>"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if protocol not https

    final String userAgent = request.getHeader("User-Agent") ;
    if ( (userAgent == null) || (! (userAgent.contains("Chrome"))) )
      {
      out.println
        (  "<h1>This application was developed &amp; tested using the browser Google Chrome, Version 36.0.1985.125 m.</h1>"
        +  "<h1>Download the Chrome web browser from http://www.Google.com/Chrome.</h1>"
        +  "<hr></body></html>"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if userAgent not Chrome

    Connection         dbConn     =  null ;
    Statement          stmnt      =  null ;
    ResultSet          rs         =  null ;
    ResultSetMetaData  rsmd       =  null ;
    String             sqlString  =  null ;
    String             compTable  =  "" ;

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
      sqlString = "SELECT * FROM MATH_RELAYS.COMPETITION ORDER BY COMPDATE DESC" ;
      rs = stmnt.executeQuery( sqlString ) ;
      rsmd = rs.getMetaData() ;

      compTable += "                <table><caption>Select Competition</caption>\n" ;
      boolean  first = true ;
      while ( rs.next() )
        {
        compTable += "                  <tr>\n" ;
        int            ID        =  rs.getInt("ID") ;
        java.sql.Date  CompDate  =  rs.getDate("CompDate") ;
        String         Name      =  rs.getString("Name") ;
        compTable += "                    "
          +  "<td><input type='radio' name='COMPETITION_ID' value='" + ID + "'"
          +  (first?" checked":"") + "></td>\n" ;
        compTable += "                    <td style='display:none;'>" + ID + "</td>\n" ;
        compTable += "                    <td>" + Name + "</td>\n" ;
        compTable += "                    <td>" + CompDate + "</td>\n" ;
        first = false ;
        } // end while
      compTable += "                </table>\n" ;
      } // end try block (try to connect to database, that is)
    catch ( URISyntaxException use )       { use.printStackTrace() ;  }
    catch ( ClassNotFoundException cnfe )  { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )            { sqle.printStackTrace() ; }
    finally
      {
      try
        {
        if ( rs     != null ) rs.close() ;
        if ( stmnt  != null ) stmnt.close() ;
        if ( dbConn != null ) dbConn.close() ;
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block

    out.println
      (  "    <form name='loginForm' action='Home' method='POST'>\n"
      +  "      <div>\n"
      +  "        <fieldset>\n"
      +  "          <legend>FHSU Math Relays User Login</legend>\n"
      +  "          <table style='border-style:none;'>\n"
      +  "            <tr>\n"
      +  "              <td>\n"
      +  "                <table><caption>Enter Username and Password</caption>\n"
      +  "                  <tr><td>Username</td>\n"
      +  "                      <td><input name='UserName' value=''\n"
      +  "                                 type='text' size='15' maxlength='15'\n"
      +  "                                 style='width:96%;'></td></tr>\n"
      +  "                  <tr><td>Password</td>\n"
      +  "                      <td><input name='Password' value='' type='password'\n"
      +  "                                 size='15' style='width:96%;'></td></tr>\n"
      +  "                </table>\n"
      +  "              </td>\n"
      +  "              <td>\n" + compTable + "              </td>\n"
      +  "            </tr>\n"
      +  "            <tr>\n"
      +  "              <td colspan='2'><input type='submit' name='ValidateUser'\n"
      +  "                               value='Login' style='width:100%;' autofocus></td>\n"
      +  "            </tr>\n"
      +  "          </table>\n"
      +  "        </fieldset>\n"
      +  "      </div>\n"
      +  "    </form>"  ) ;

    // print closing tags of the HTML webpage ...
    out.println ( "  <hr></body>\n</html>" ) ;
    out.flush() ;
    out.close() ;
    } // end doPost method

  } // end LoginServlet class
