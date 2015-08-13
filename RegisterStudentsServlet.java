/*
 *  File: RegisterStudentsServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of the doGet & doPost methods that are executed
 *  when the URL pattern is ".../RegisterStudents".  GET requests are forwarded to
 *  the doPost method.  The doPost method generates the web page that permits faculty
 *  sponsors to register their students for Math Relays events.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.net.* ;
import  java.sql.* ;
import  java.util.* ;
import  org.jsoup.Jsoup ;
import  org.jsoup.nodes.Document;
import  org.jsoup.nodes.Element;
import  org.jsoup.select.Elements;

@WebServlet  (  name = "RegisterStudentsServlet",  urlPatterns = {"/RegisterStudents"}  )

public class RegisterStudentsServlet extends HttpServlet
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
    boolean  isFirst = true ;
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

    // ---- declare Jsoup-related variables for database DELETEs, UPDATEs, and INSERTs ----
    Elements  deleted    =  null ;
    Elements  updated    =  null ;
    Elements  inserted   =  null ;
    String    htmlTable  =  request.getParameter("htmlTable") ;
    if ( htmlTable != null )
      {
      Document doc = Jsoup.parseBodyFragment(htmlTable) ;
      deleted  =  doc.select("tr.dataRow.deleted") ;
      updated  =  doc.select("tr.dataRow.updated:not(.inserted)") ;
      inserted =  doc.select("tr.dataRow.inserted") ;
      } // end if

    // ---- declare database-related variables ----
    Connection         dbConn         =  null ;
    DatabaseMetaData   dbmd           =  null ;
    Statement          stmnt          =  null ;
    Statement          intraRowStmnt  =  null ;
    String             sqlString      =  null ;
    ResultSet          rs             =  null ;
    ResultSet          intraRowRS     =  null ;
    ResultSetMetaData  rsmd           =  null ;

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
      dbmd  =  dbConn.getMetaData() ;
      stmnt = dbConn.createStatement() ;

      // ---- perform SQL deletes, updates, and inserts ----
      if ( (deleted != null) && (deleted.size() > 0) )
        {
        sqlString  =  "DELETE FROM Math_Relays.Student " ;
        // as of 9-JUL-2014, deleting from database table Math_Relays.Student
        // cascades into deletion(s) from Student_School & Student_Team
        isFirst = true ;
        Iterator<Element> trIter = deleted.iterator() ;
        while ( trIter.hasNext() )
          {
          Element trToDelete = trIter.next() ;
          String studentID  =  trToDelete.select("td[data-table-name=Student][data-column-name=ID]").first().text() ;
          sqlString += ( isFirst ? "WHERE " : "OR " ) ;
          sqlString += "(ID = " + studentID + ") " ;
          isFirst = false ;
          } // end while another TR to delete
        stmnt.addBatch ( sqlString ) ;
        } // end deletes

      // ---- SQL updates ----
      if ( (updated != null) && (updated.size() > 0) )
        {
        Iterator<Element> trIter = updated.iterator() ;
        while ( trIter.hasNext() )
          {
          Element trToUpdate = trIter.next() ;
          String  studentID  =  trToUpdate.select("td[data-table-name=Student][data-column-name=ID]").first().text() ;
          String  gradeID  =  trToUpdate.select("td[data-table-name=Student_School][data-column-name=Grade_ID] > select option[selected]").first().attr("value") ;
          String  lastName  =  trToUpdate.select("td[data-table-name=Student][data-column-name=LastName] > input").first().attr("value") ;
          String  firstName  =  trToUpdate.select("td[data-table-name=Student][data-column-name=FirstName] > input").first().attr("value") ;
          String  competitionID  =  trToUpdate.select("td[data-table-name=Student_School][data-column-name=Competition_ID]").first().text() ;
          sqlString  =  "UPDATE  Math_Relays.Student "
                     +  "SET     LastName='" + lastName + "', FirstName='" + firstName + "' "
                     +  "WHERE   (ID = " + studentID + ") "  ;
          stmnt.addBatch ( sqlString ) ;
          sqlString  =  "UPDATE  Math_Relays.Student_School "
                     +  "SET     Grade_ID=" + gradeID + " "
                     +  "WHERE   ( (Competition_ID = " + competitionID + ") "
                     +  "AND       (Student_ID = " + studentID + ") ) "  ;
          stmnt.addBatch ( sqlString ) ;

          Elements elems = trToUpdate.select("td[data-event-id]>select.eventteam option[selected]") ;
          for ( Element elem : elems )
            {
            Element tdElem = elem.parent().parent() ;
            String eventIdString = tdElem.attr("data-event-id") ;
            String groupSelection = elem.attr("value") ;
            String gradeSelection = tdElem.parent().select("td[data-event-id="+eventIdString+"]>select.eventgrade option[selected]").first().attr("value") ;
            if ( (groupSelection == null) || groupSelection.isEmpty() )
              {
              sqlString  =  "DELETE FROM Math_Relays.Student_Event "
                         +  "WHERE ( (Student_ID="+studentID+") AND (Event_ID="+eventIdString+") ) " ;
              stmnt.addBatch ( sqlString ) ;
              }
            else if ( groupSelection.length() == 1 )
              {
              sqlString = "INSERT INTO Math_Relays.Student_Event ( Student_ID, Event_ID, Grade_ID, Grouping ) "
                        + "SELECT "+studentID+", "+eventIdString+", "+gradeSelection+", '"+groupSelection+"' "
                        + "WHERE ( NOT EXISTS ( SELECT Student_ID FROM Math_Relays.Student_Event "
                        + "                     WHERE ((Student_ID="+studentID+") AND (Event_ID="+eventIdString+")) ) ) " ;
              stmnt.addBatch ( sqlString ) ;
              sqlString  =  "UPDATE  Math_Relays.Student_Event "
                         +  "SET     Grade_ID=" + gradeSelection + ", Grouping='" + groupSelection + "' "
                         +  "WHERE   ( (Student_ID = " + studentID + ") "
                         +  "AND       (Event_ID = " + eventIdString + ") ) "  ;
              stmnt.addBatch ( sqlString ) ;
              } // end if
            } // end for
          } // end while next TR to update
        } // end updates

      // ---- SQL inserts ----
      if ( (inserted != null) && (inserted.size() > 0) )
        {
        Iterator<Element> trIter = inserted.iterator() ;
        while ( trIter.hasNext() )
          {
          Element trToInsert = trIter.next() ;
          String lastName = trToInsert.select("td[data-table-name=Student][data-column-name=LastName] > input").attr("value") ;
          String firstName = trToInsert.select("td[data-table-name=Student][data-column-name=FirstName] > input").attr("value") ;
          String competitionID = trToInsert.select("td[data-table-name=Student_School][data-column-name=Competition_ID]").first().text() ;
          String schoolID = trToInsert.select("td[data-table-name=Student_School][data-column-name=School_ID]").first().text() ;
          String gradeID  =  trToInsert.select("td[data-table-name=Student_School][data-column-name=Grade_ID] > select option[selected]").first().attr("value") ;

          // ---- insert one new tuple into each of Student (parent) and Student_School (child) ----
          // use Common Table Expression; see http://www.postgresql.org/docs/9.3/static/queries-with.html
          sqlString  =  ""
            +  "WITH CTE AS ( INSERT INTO Math_Relays.Student(LastName,FirstName) "
            +  "              VALUES('" + lastName + "','" + firstName + "') "
            +  "              RETURNING " + competitionID + ", ID, " + schoolID + ", " + gradeID + " ) "
            +  "  INSERT INTO Math_Relays.Student_School ( Competition_ID, Student_ID, School_ID, Grade_ID ) "
            +  "  SELECT * FROM CTE "  ;
          stmnt.addBatch ( sqlString ) ;

          Elements elems = trToInsert.select("td[data-event-id]>select.eventteam option[selected]") ;
          for ( Element elem : elems )
            {
            Element tdElem = elem.parent().parent() ;
            String eventIdString = tdElem.attr("data-event-id") ;
            String groupSelection = elem.attr("value") ;
            String gradeSelection = tdElem.parent().select("td[data-event-id="+eventIdString+"]>select.eventgrade option[selected]").first().attr("value") ;
            if ( groupSelection.length() == 1 )
              {
              sqlString = "INSERT INTO Math_Relays.Student_Event ( Student_ID, Event_ID, Grade_ID, Grouping ) "
                        + "VALUES ( (SELECT(currval('Math_Relays.Student_Id_Seq'))), " + eventIdString + ", " + gradeSelection + ", '" + groupSelection + "' ) " ;
              stmnt.addBatch ( sqlString ) ;
              } // end if
            } // end for
          } // end while
        } // end inserts

      // ---- commit SQL transactions to database ----
      try
        {
        int[] updateCount = stmnt.executeBatch();
        dbConn.commit() ;
        for ( int i = 0 ; i < updateCount.length ; i ++ )
          assert (     ((updateCount[i] >= 0) || (updateCount[i] == Statement.SUCCESS_NO_INFO))
                    && (updateCount[i] != Statement.EXECUTE_FAILED) ) ;
        } // end try block
      catch ( BatchUpdateException bue ) // a subclass of java.sql.SQLException
        {
        SESSION.invalidate() ;
        out.print ( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n" ) ;
        out.print ( "<title>Math Relays</title>\n</head>\n<body>\n" ) ;
        SQLException currentExcept = bue ;
        do
          out.print ( "    <h4>" + currentExcept.getMessage() + "</h4>\n" ) ;
        while ( (currentExcept = currentExcept.getNextException()) != null ) ;
        out.print ( "  </body>\n</html>\n" ) ;
        out.flush();
        out.close() ;
        return ; // exit doPost method
        } // end catch block

      // ---- generate HTML to be sent to browser ----
      out.println ( "<!DOCTYPE html>" ) ;
      out.println ( "<html>" ) ;
      out.println ( "  <head>" ) ;
      out.println ( "    <meta charset='UTF-8'>" ) ;
      out.println ( "    <title>Math Relays</title>" ) ;
      out.println ( "    <link rel='stylesheet' type='text/css' href='Math_Relays.css'>" ) ;
      out.println ( "    <script src='https://code.jquery.com/jquery-latest.js'></script>" ) ;
      out.println ( "    <script src='Math_Relays.js'></script>" ) ;
      out.println ( "  </head>" ) ;
      out.println ( "  <body>" ) ;

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

      // ---- create template for <tr> elements --
      final String trTemplateString = ""
        +  "<tr id='templateRow' class='not_displayed'>\n"
        +  "  <td><button type='button' class='deleteButton'>&nbsp;Delete&nbsp;</button></td>\n"
        +  "  <td data-table-name='Student' data-column-name='ID' data-column-type='4' class='not_displayed'></td>\n"
        +  "  <td data-table-name='Student' data-column-name='LastName' data-column-type='12'>\n"
        +  "    <input type='text' value='' placeholder='Last name'>\n"
        +  "  </td>\n"
        +  "  <td data-table-name='Student' data-column-name='FirstName' data-column-type='12'>\n"
        +  "    <input type='text' value='' placeholder='First name'>\n"
        +  "  </td>\n"
        +  "  <td data-table-name='Student_School' data-column-name='School_ID' data-column-type='4' class='not_displayed'>" + associatedSchool.getID() + "</td>\n"
        +  "  <td data-table-name='School' data-column-name='Name' data-column-type='12' class='not_displayed'>\n"
        +  "    <input type='text' value='" + associatedSchool.getName() + "' readonly>\n"
        +  "  </td>\n"
        +  "  <td data-table-name='Student_School' data-column-name='Competition_ID' data-column-type='4' class='not_displayed'>" + selectedCompetition.getID() + "</td>\n"
        +  "  <td data-table-name='Competition' data-column-name='CompDate' data-column-type='91' class='not_displayed'>" + selectedCompetition.getCompDate() + "</td>\n"
        +  "  <td data-table-name='Competition' data-column-name='Name' data-column-type='12' class='not_displayed'>\n"
        +  "    <input type='text' value='" + selectedCompetition.getName() + "' readonly>\n"
        +  "  </td>\n"
        +  "  <td data-table-name='Student_School' data-column-name='Student_ID' data-column-type='4' class='not_displayed'></td>\n"
        +  "  <td data-table-name='Student_School' data-column-name='Grade_ID' data-column-type='4'>\n"
        +  "    <select class='grade'>\n"
        +  "      <option value='9' selected>9</option>\n"
        +  "      <option value='10'>10</option>\n"
        +  "      <option value='11'>11</option>\n"
        +  "      <option value='12'>12</option>\n"
        +  "    </select>\n"
        +  "  </td>\n"
        +  "</tr>\n" ;
      Document doc = Jsoup.parseBodyFragment ( "<table>" + trTemplateString + "</table>" ) ;
      Element  trTemplate = doc.select("tr").first() ;

      final String teamTemplateString = ""
        +  "  <td>\n"
        +  "    <select class='eventteam'>\n"
        +  "      <option value='' selected></option>\n"
        +  "      <option value='I'>I</option>\n"
        +  "      <option value='A'>A</option>\n"
        +  "      <option value='B'>B</option>\n"
        +  "    </select>\n"
        +  "  </td>\n" ;
      final String gradeTemplateString = ""
        +  "  <td>\n"
        +  "    <select class='eventgrade' data-min-grade='9' data-max-grade='12' style='visibility:hidden;opacity:0.0;'>\n"
        +  "      <option value='9' selected>9</option>\n"
        +  "      <option value='10'>10</option>\n"
        +  "      <option value='11'>11</option>\n"
        +  "      <option value='12'>12</option>\n"
        +  "    </select>\n"
        +  "  </td>\n" ;

      // ---- obtain event names from database ----
      List<Event> eventList = new ArrayList<Event> () ;
      sqlString  =  "SELECT * FROM Math_Relays.Event WHERE (Competition_ID = " + selectedCompetition.getID() + ") ORDER BY ID " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      while ( rs.next() )
        {
        Event event = new Event ( rs.getInt("ID"), rs.getInt("Competition_ID"), rs.getString("Name"), rs.getInt("MinGrade"), rs.getInt("MaxGrade") ) ;
	    eventList.add(event) ;
        // --------------------------------------
        doc = Jsoup.parseBodyFragment ( "<table><tr>" + teamTemplateString + "</tr></table>" ) ;
        Element  teamTemplate = doc.select("td").first() ;
        teamTemplate.attr ( "data-event-id", Integer.toString(event.getID()) ) ;
        trTemplate.appendChild ( teamTemplate ) ;
        // --------------------------------------
        doc = Jsoup.parseBodyFragment ( "<table><tr>" + gradeTemplateString + "</tr></table>" ) ;
        Element  gradeTemplate = doc.select("td").first() ;
        gradeTemplate.attr ( "data-event-id", Integer.toString(event.getID()) ) ;
        gradeTemplate.attr ( "data-min-grade", Integer.toString(event.getMinGrade()) ) ;
        gradeTemplate.attr ( "data-max-grade", Integer.toString(event.getMaxGrade()) ) ;
        trTemplate.appendChild ( gradeTemplate ) ;
        } // end while
      if ( rs != null ) { rs.close(); rs = null; }

      // ---- generate table of datarows plus Add and Save buttons ----
      out.println ( "<form method='POST' action='RegisterStudents' onsubmit='return validateFormData(this)'>" ) ;
      out.println ( "  <input type='hidden' id='hiddenInput' name='htmlTable' value=''>\n" ) ;
      out.println ( "  <div>" ) ;
      out.println ( "    <fieldset>" ) ;
      out.println ( "      <legend>&nbsp;&nbsp;Students Representing " + associatedSchool.getName()
                  + " in the " + selectedCompetition.getName() + "&nbsp;&nbsp;</legend>" ) ;
      out.println ( "      <input id='submitButton' type='submit' value='Add, Modify, or Delete Rows, Then Click Here to Save Changes' style='font-style:italic;font-weight:bold;color:red;'><hr>" ) ;
      out.println ( "      <button type='button' id='insertButton' style='width:100%;font-style:italic;'>Click Here to Add a Student</button><hr>" ) ;
      out.println ( "<table id='mathRelaysTable'>" ) ;
      out.println ( "<thead>" ) ;
      out.println ( "<tr class='headerRow'>" ) ;
      out.println ( "  <th rowspan='3'>Action</th>" ) ;
      out.println ( "  <th rowspan='3'>Last Name</th>" ) ;
      out.println ( "  <th rowspan='3'>First Name</th>" ) ;
      out.println ( "  <th rowspan='3'>Current Grade</th>" ) ;
      if ( eventList.size() > 0 )
        out.println ( "  <th colspan='" + (2 * eventList.size()) + "'>Events</th>" ) ;
      out.println ( "</tr>" ) ;
      out.println ( "<tr class='headerRow' style='font-size:smaller;'>" ) ;
      Iterator<Event> eventIter = eventList.iterator() ;
      while ( eventIter.hasNext() )
        {
        Event event = eventIter.next() ;
        out.println ( "  <th colspan='2' style='width:11%'>" + event.getName() + "</th>" ) ;
        } // end while
      out.println ( "</tr>" ) ;
      out.println ( "<tr style='font-size:smaller;'>" ) ;
      for ( int i = 1 ; i <= eventList.size() ; i ++ )
        out.println ( "<th>Team</th><th>Grade</th>" ) ;
      out.println ( "</tr>" ) ;
      out.println ( "</thead>" ) ;
      out.println ( "<tbody>" ) ;
      out.println ( trTemplate.toString() ) ;

      // -- change trTemplate from a templateRow to a dataRow for future use below --
      trTemplate.removeClass("not_displayed").addClass("dataRow").removeAttr("id") ;

      // ---- obtain HTML table data from database table ----
      sqlString = "SELECT * FROM Math_Relays.Student__Student_School(" + associatedSchool.getID() + "," + selectedCompetition.getID() + ") " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      rsmd = rs.getMetaData();

      // ---- loop through ResultSet, creating data rows of HTML table ----
      while ( rs.next() )
        {
        // -- populate trTemplate with data from ResultSet --
        String studentID = rs.getString(1) ;
        trTemplate.select("td[data-table-name=Student][data-column-name=ID]").first().text(studentID) ;
        trTemplate.select("td[data-table-name=Student][data-column-name=LastName] > input").attr( "value", rs.getString(2) ) ;
        trTemplate.select("td[data-table-name=Student][data-column-name=FirstName] > input").attr( "value", rs.getString(3) ) ;
        trTemplate.select("td[data-table-name=Student_School][data-column-name=School_ID]").first().text(rs.getString(4)) ;
        trTemplate.select("td[data-table-name=School][data-column-name=Name] > input").attr( "value", rs.getString(5) ) ;
        trTemplate.select("td[data-table-name=Student_School][data-column-name=Competition_ID]").first().text(rs.getString(6)) ;
        trTemplate.select("td[data-table-name=Competition][data-column-name=CompDate]").first().text(rs.getString(7)) ;
        trTemplate.select("td[data-table-name=Competition][data-column-name=Name] > input").attr( "value", rs.getString(8) ) ;
        trTemplate.select("td[data-table-name=Student_School][data-column-name=Student_ID]").first().text(rs.getString(9)) ;
        Elements options = trTemplate.select("td[data-table-name=Student_School][data-column-name=Grade_ID] > select > option") ;
        String currentGrade = rs.getString(10) ;
        for ( Element option : options )
          {
          if ( option.attr("value") .equals (currentGrade) )
            option.attr ( "selected", "selected" ) ;
          else
            option.removeAttr ( "selected" ) ;
          } // end for

        // ---- populate the Team & Grade drop-down select element for this student ----
        sqlString  =  ""
          +  "SELECT     ST_ID, EV_ID, EV_MIN_GR, EV_MAX_GR, SE.Grade_ID, SE.Grouping "
          +  "FROM       ( SELECT *    FROM (SELECT " + studentID + " AS ST_ID) AS ALIAS_1 "
          +  "             CROSS JOIN  ( SELECT  ID AS EV_ID, MinGrade AS EV_MIN_GR, MaxGrade AS EV_MAX_GR "
          +  "                           FROM    Math_Relays.EVENT "
          +  "                           WHERE Competition_ID = " + selectedCompetition.getID() + " "
          +  "                           ORDER BY ID ) AS ALIAS_2 ) "
          +  "              AS ALIAS_3 "
          +  "LEFT JOIN  Math_Relays.Student_Event SE "
          +  "ON         ( (SE.Student_ID = ST_ID) AND (SE.Event_ID = EV_ID) ) "
          +  "WHERE      (ST_ID = " + studentID + ") "
          +  "ORDER BY   EV_ID "  ;
        intraRowStmnt = dbConn.createStatement() ;
        intraRowRS = intraRowStmnt.executeQuery ( sqlString ) ;
        while ( intraRowRS.next() )
          {
          int st_id = intraRowRS.getInt("ST_ID") ;
          int ev_id = intraRowRS.getInt("EV_ID") ;
          int ev_min_gr = intraRowRS.getInt("EV_MIN_GR") ;
          int ev_max_gr = intraRowRS.getInt("EV_MAX_GR") ;
          int grade_id = intraRowRS.getInt("Grade_ID") ; // 0 if NULL
          boolean gradeIdIsNull = intraRowRS.wasNull() ;
          char[] charBuffer = new char[1] ;
          java.io.Reader reader = intraRowRS.getCharacterStream("Grouping") ; // null if NULL
          char grouping = '\0' ;
          boolean groupingIsNull = true ;
          if ( ! intraRowRS.wasNull() )
            {
            reader.read(charBuffer) ;
            grouping = charBuffer[0] ;
            groupingIsNull = false ;
            } // end if
          Element selectElem = trTemplate.select("td[data-event-id="+ ev_id +"]>select.eventteam").first() ;
          options = selectElem.select("option") ;
          for ( Element option : options )
            {
            option.removeAttr ( "selected" ) ;
            String optionValue = option.attr("value") ;
            String groupString = Character.toString(grouping) ;
            if ( groupingIsNull && (optionValue.length() == 0) )
              option.attr ( "selected", "selected" ) ;
            else if ( (! groupingIsNull) && (optionValue.equals(groupString)) )
              {
              option.attr ( "selected", "selected" ) ;
              } // end else if
            } // end for

          // adjust selectedGrade according to this event's min grade & max grade
          int currGrInt = Integer.parseInt(currentGrade) ;
          int selectedGrade = -1 ;
          // (grade_id could be 0, 9, 10, 11, or 12)
          if ( gradeIdIsNull )
            selectedGrade = ev_min_gr ;
          else
            selectedGrade = grade_id ;
          selectedGrade = ( (ev_min_gr > selectedGrade) ? (ev_min_gr) : (selectedGrade) ) ;
          selectedGrade = ( (ev_max_gr < selectedGrade) ? (ev_max_gr) : (selectedGrade) ) ;
          selectElem = trTemplate.select("td[data-event-id="+ ev_id +"]>select.eventgrade").first() ;
          if ( groupingIsNull )
            selectElem.attr("style","visibility:hidden;opacity:0.0;") ;
          else
            selectElem.attr("style","visibility:visible;opacity:1.0;") ;

          // eliminate all options that are either below min grade or above max grade
          options = selectElem.select("option") ;
          for ( Element option : options )
            {
            String optionValue = option.attr("value") ;
            int optionInt = Integer.parseInt( optionValue ) ;
            if ( (optionInt < ev_min_gr) || (optionInt > ev_max_gr) )
              option.remove() ;
            else
              {
              if ( optionValue .equals ( Integer.toString(selectedGrade) ) )
                option.attr ( "selected", "selected" ) ;
              else
                option.removeAttr ( "selected" ) ;
              }
            } // end for
          } // end while
        if ( intraRowRS    != null )  { intraRowRS.close();      intraRowRS = null;     }
        if ( intraRowStmnt != null )  { intraRowStmnt.close();   intraRowStmnt = null;  }

        // ---- now fully populated, trTemplate inserted into HTML ----
        out.println ( trTemplate.toString() ) ;
        } // end while
      if ( rs != null ) { rs.close(); rs = null; }
      out.println ( "</tbody>" ) ;
      out.println ( "</table>" ) ;
      out.println ( "    </fieldset>" ) ;
      out.println ( "  </div>" ) ;
      out.println ( "</form>\n<hr>\n</body>\n</html>" ) ;
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
        if ( rs      !=  null )  { rs.close();      rs = null;     }
        if ( stmnt   !=  null )  { stmnt.close();   stmnt = null;  }
        if ( dbConn  !=  null )  { dbConn.close();  dbConn = null; }
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block
    } // end doPost method

  } // end RegisterStudentsServlet class
