/*
 *  File: LogoutServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of the doGet & doPost methods that are executed
 *  when the URL pattern is ".../Logout".  This corresponds to the "logout page",
 *  a web page a system user sees upon logging out of the system.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;

@WebServlet  (  name = "LogoutServlet",  urlPatterns = {"/Logout"}  )

public class LogoutServlet extends HttpServlet
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
    final HttpSession  SESSION    =  request.getSession() ;
    SysUser            loginUser  =  (SysUser) SESSION.getAttribute("LOGIN_USER") ;
    SESSION.invalidate() ;
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;
    out.println (
         "<!DOCTYPE html>\n"
      +  "<html>\n"
      +  "  <head>\n"
      +  "    <meta charset='UTF-8'>\n"
      +  "    <title>FHSU Math Relays</title>\n"
      +  "  </head>\n"
      +  "  <body>\n"
      +  "    <hr>\n" ) ;
    if ( loginUser == null )
      out.println ( "    <h1>No user is currently logged in.</h1>" ) ;
    else
      out.println ( "    <h1>" + loginUser.getUserName() + " has logged out.</h1>" ) ;
    out.println (
         "    <hr>\n"
      +  "  </body>\n"
      +  "</html>\n" ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
    } // end doPost method

  } // end LogoutServlet class
