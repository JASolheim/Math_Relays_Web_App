/*
 *  File: TableMaintenanceServlet.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains definitions of the doPost method that is executed
 *  when the URL pattern is ".../TableMaintenance".  The doPost method generates the
 *  web page that permits a system administrator to perform database operations
 *  on the tables of the Math_Relays schema.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.net.URI ;
import  java.net.URISyntaxException ;
import  java.sql.* ;
import  java.util.* ;
import  org.jsoup.Jsoup ;
import  org.jsoup.nodes.Document;
import  org.jsoup.nodes.Element;
import  org.jsoup.select.Elements;

@WebServlet  (  name = "TableMaintenanceServlet",  urlPatterns = {"/TableMaintenance"}  )

public class TableMaintenanceServlet extends HttpServlet
  {

  @Override
  protected void doPost ( HttpServletRequest request, HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;

    final HttpSession  SESSION           =  request.getSession() ;
    final SysUser      LOGIN_USER        =  (SysUser) SESSION.getAttribute("LOGIN_USER") ;
    final Role         LOGIN_ROLE        =  (Role) SESSION.getAttribute("LOGIN_ROLE") ;
    final Competition  COMPETITION       =  (Competition) SESSION.getAttribute("COMPETITION") ;
    final String       TABLENAME         =  request.getParameter("Table") ;
    School             associatedSchool  =  (School) SESSION.getAttribute("ASSOCIATED_SCHOOL") ;

    if ( (LOGIN_USER == null) || (LOGIN_ROLE == null) || (COMPETITION == null) || (TABLENAME == null) )
      {
      SESSION.invalidate() ;
      out.println (  "<!DOCTYPE html>\n<html><body><h1>No user is currently logged in.</h1></body></html>" ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if

    final boolean      VALID_TABLE  =
          (LOGIN_ROLE.getDescription() .equalsIgnoreCase ("Administrator"))
      ||  (TABLENAME .equals ("STUDENT"))  ||  (TABLENAME .equals ("STUDENT_SCHOOL"))
      ||  (TABLENAME .equals ("TEAM"))  ||  (TABLENAME .equals ("STUDENT_TEAM"))
      ||  (TABLENAME .equals ("TEAM_EVENT")) ;

    if ( ! VALID_TABLE )
      {
      SESSION.invalidate() ;
      out.println (  "<!DOCTYPE html>\n<html><body><h1>Invalid table.</h1></body></html>" ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if

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

    // later, check to be sure the ResultSets, etc. are all closed ...
    Connection                dbConn            =  null ;
    DatabaseMetaData          dbmd              =  null ;
    Map<Short,String>         pkMap             =  null ;
    ResultSet                 primaryKeys       =  null ;
    Statement                 sqlStatement      =  null ;
    String                    sqlString         =  null ;
    ResultSet                 tableRows         =  null ;
    ResultSetMetaData         rsmd              =  null ;
    Map<String,List<String>>  fkMap             =  null ;
    ResultSet                 importedKeys      =  null ;
    ResultSet                 foreignKeyValues  =  null ;
    String                    tableHeaderRow    =  null ;
    String                    templateRow       =  null ;
    Map<String,Element>       columnMap         =  null ;

    try
      {
      URI dbUri = new URI(System.getenv("DATABASE_URL")) ;
      String username = dbUri.getUserInfo().split(":")[0] ;
      String password = dbUri.getUserInfo().split(":")[1] ;
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() ;
      Class.forName ( "org.postgresql.Driver" ) ;
      dbConn = DriverManager.getConnection ( dbUrl, username, password ) ;

      dbConn.setAutoCommit ( false ) ;
      dbmd  =  dbConn.getMetaData() ;
      sqlStatement = dbConn.createStatement() ;

      if ( (deleted != null) && (deleted.size() > 0) )
        {
        sqlString = "DELETE FROM math_relays." + TABLENAME + " WHERE " ;
        boolean isFirstTr = true ;
        Iterator<Element> trIter = deleted.iterator() ;
        while ( trIter.hasNext() )
          {
          if ( ! isFirstTr )
            sqlString += " OR " ;
          isFirstTr = false ;
          Element trToDelete = trIter.next() ;
          Elements  tdElems    =  trToDelete.select("td") ;
          Iterator<Element> tdIter = tdElems.iterator() ;
          String conjunction = "(" ;
          boolean isFirstTd = true ;
          while ( tdIter.hasNext() )
            {
            Element tdElem = tdIter.next() ;
            Map<String,String> dataset = tdElem.dataset() ;
            String columnName = dataset.get("column-name") ;
            boolean isPrimaryKey = (null != dataset.get("primary-key")) ;
            if ( (columnName != null) && isPrimaryKey )
              {
              if ( ! isFirstTd )
                conjunction += " AND " ;
              isFirstTd = false ;
              Elements selects = tdElem.select("select option[selected]") ;
              Elements inputs = null ;
              String selection = null ;
              if ( (selects != null) && (selects.size() > 0) )
                selection  =  selects.first().text() ;
              else
                {
                inputs = tdElem.select("input") ;
                selection  =  inputs.first().attr("value") ;
                }
              int columnType = Integer.parseInt ( dataset.get("column-type") ) ;
              switch ( columnType )
                {
                case java.sql.Types.CHAR :
                case java.sql.Types.VARCHAR :
                case java.sql.Types.DATE :
                  selection = ("'" + selection + "'") ;
                  break ;
                } // end switch
              conjunction += ("(" + columnName + "=" + selection + ")") ;
              } // end if
            } // end while loop
          conjunction += ")" ;
          sqlString += (conjunction + " ") ;
          } // end while
        sqlStatement.addBatch ( sqlString ) ;
        } // end deletes

      if ( (updated != null) && (updated.size() > 0) )
        {
        Iterator<Element> trIter = updated.iterator() ;
        while ( trIter.hasNext() )
          {
          String setClause = null ;
          String whereClause = null ;
          Element trToUpdate = trIter.next() ;
          Elements  tdElems    =  trToUpdate.select("td") ;
          Iterator<Element> tdIter = tdElems.iterator() ;
          while ( tdIter.hasNext() )
            {
            Element tdElem = tdIter.next() ;
            Map<String,String> dataset = tdElem.dataset() ;
            String columnName = dataset.get("column-name") ;
            if ( columnName != null )
              {
              int columnType = Integer.parseInt ( dataset.get("column-type") ) ;
              Elements selects = tdElem.select("select option[selected]") ;
              Elements inputs = null ;
              String selection = null ;
              if ( (selects != null) && (selects.size() > 0) )
                selection  =  selects.first().text() ;
              else
                {
                inputs = tdElem.select("input") ;
                selection  =  inputs.first().attr("value") ;
                } // end else
              switch ( columnType )
                {
                case java.sql.Types.CHAR :
                case java.sql.Types.VARCHAR :
                case java.sql.Types.DATE :
                  selection = ("'" + selection + "'") ;
                  break ;
                } // end switch
              boolean isPrimaryKey = (null != dataset.get("primary-key")) ;
              if ( isPrimaryKey )
                if ( whereClause == null )
                  whereClause = (" WHERE (" + columnName + "=" + selection + ") ") ;
                else				  
                  whereClause += (" AND (" + columnName + "=" + selection + ") ") ;
              else
                {
                if ( setClause == null )
                  setClause = (" SET " + columnName + "=" + selection) ;
                else				  
                  setClause += (", " + columnName + "=" + selection) ;
                }
              } // end if
            } // end while
          sqlString =   "UPDATE math_relays." + TABLENAME + setClause + whereClause + " " ;
          sqlStatement.addBatch ( sqlString ) ;
          } // end while
        } // end updates

      if ( (inserted != null) && (inserted.size() > 0) )
        {
        sqlString = "INSERT INTO math_relays." + TABLENAME  ;
        String columnHeaderTuple = "" ;
        String columnValueTuples = "" ;
        boolean isFirstTr = true ;
        Iterator<Element> trIter = inserted.iterator() ;
        while ( trIter.hasNext() )
          {
          String columnValueTuple = (isFirstTr ? ("VALUES ") : (", ")) ;
          Element trToInsert = trIter.next() ;
          Elements  tdElems    =  trToInsert.select("td") ;
          Iterator<Element> tdIter = tdElems.iterator() ;
          boolean isFirstCol = true ;
          while ( tdIter.hasNext() )
            {
            Element tdElem = tdIter.next() ;
            Map<String,String> dataset = tdElem.dataset() ;
            String columnName = dataset.get("column-name") ;
            boolean isAutoIncrement = (null != dataset.get("auto-increment")) ;
            if ( (columnName != null) && (! isAutoIncrement) )
              {
              if ( isFirstTr )
                {
                columnHeaderTuple += (isFirstCol ? " ( " : ", " ) ;
                columnHeaderTuple += columnName ;
                }
              columnValueTuple += (isFirstCol ? "( " : ", " ) ;
              int columnType = Integer.parseInt ( dataset.get("column-type") ) ;
              Elements selects = tdElem.select("select option[selected]") ;
              Elements inputs = null ;
              String selection = null ;
              if ( (selects != null) && (selects.size() > 0) )
                selection  =  selects.first().text() ;
              else
                {
                inputs = tdElem.select("input") ;
                selection  =  inputs.first().attr("value") ;
                }
              switch ( columnType )
                {
                case java.sql.Types.CHAR :
                case java.sql.Types.VARCHAR :
                case java.sql.Types.DATE :
                  selection = ("'" + selection + "'") ;
                  break ;
                } // end switch
              columnValueTuple += selection ;
              isFirstCol = false ;
              } // end if
            } // end while
          if ( isFirstTr ) columnHeaderTuple += " ) " ;
          columnValueTuples += (columnValueTuple + " )") ;
          isFirstTr = false ;
          } // end while
        sqlString += (columnHeaderTuple + columnValueTuples + " ") ;
        sqlStatement.addBatch ( sqlString ) ;
        } // end inserts

      try
        {
        int[] updateCount = sqlStatement.executeBatch();
        dbConn.commit() ;
        for ( int i = 0 ; i < updateCount.length ; i ++ )
          assert (     ((updateCount[i] >= 0) || (updateCount[i] == Statement.SUCCESS_NO_INFO))
                    && (updateCount[i] != Statement.EXECUTE_FAILED) ) ;
        } // end try block
      catch ( BatchUpdateException bue ) // a subclass of java.sql.SQLException
        {
        out.print ( "<!DOCTYPE html>\n" ) ;
        out.print ( "<html>\n" ) ;
        out.print ( "  <head>\n" ) ;
        out.print ( "    <meta charset='UTF-8'>\n" ) ;
        out.print ( "    <title>Math Relays " + TABLENAME + "</title>\n" ) ;
        out.print ( "  </head>\n" ) ;
        out.print ( "  <body>\n" ) ;
        SQLException currentExcept = bue ;
        do
          out.print ( "    <h4>" + currentExcept.getMessage() + "</hr>\n" ) ;
        while ( (currentExcept = currentExcept.getNextException()) != null ) ;
        out.print ( "  </body>\n" ) ;
        out.print ( "</html>\n" ) ;
        out.flush();
        out.close() ;
        SESSION.invalidate() ;
        } // end catch block

      pkMap = new HashMap<Short,String> () ;
      primaryKeys = dbmd.getPrimaryKeys( null, "math_relays", TABLENAME.toLowerCase() ) ;
      while ( primaryKeys.next() )
        {
        Short  keySeq = primaryKeys.getShort("KEY_SEQ");
        String colName = primaryKeys.getString("COLUMN_NAME");
        pkMap.put(keySeq,colName) ;
        } // end while loop
      if ( primaryKeys != null )  primaryKeys.close() ;

      fkMap = new HashMap<String,List<String>> () ;
      importedKeys = dbmd.getImportedKeys( null, "math_relays", TABLENAME.toLowerCase() ) ;
      while ( importedKeys.next() )
        {
        final String FKCOLUMN_NAME = importedKeys.getString("FKCOLUMN_NAME") ;
        fkMap.put(FKCOLUMN_NAME,new ArrayList<String>()) ;
        final String PKTABLE_NAME = importedKeys.getString("PKTABLE_NAME") ;
        final String PKCOLUMN_NAME = importedKeys.getString("PKCOLUMN_NAME") ;
        sqlString = "SELECT " + PKCOLUMN_NAME + " FROM " + "math_relays" + "." + PKTABLE_NAME + " ORDER BY " + PKCOLUMN_NAME + " ASC " ;
        foreignKeyValues  =  sqlStatement.executeQuery ( sqlString ) ;
        while ( foreignKeyValues.next() )
          {
          String datum = foreignKeyValues.getString(PKCOLUMN_NAME) ;
          fkMap.get(FKCOLUMN_NAME).add(datum) ;
          } // end while
        if ( foreignKeyValues != null )  foreignKeyValues.close() ;
        } // end while loop

      out.print ( "<!DOCTYPE html>\n" ) ;
      out.print ( "<html>\n" ) ;
      out.print ( "  <head>\n" ) ;
      out.print ( "    <meta charset='UTF-8'>\n" ) ;
      out.print ( "    <title>Math Relays " + TABLENAME + "</title>\n" ) ;
      out.print ( "    <link rel='stylesheet' type='text/css' href='Math_Relays.css'>\n" ) ;
      out.print ( "    <script src='https://code.jquery.com/jquery-latest.js'></script>\n" ) ;
      out.print ( "    <script src='TableMaintenance.js'></script>\n" ) ;
      out.print ( "  </head>\n" ) ;
      out.print ( "  <body><hr>\n" ) ;

      // ---- display logged-in user's information and logout button ----
      out.println ( "<hr><table style='border-style:none;'><tr><td><table style='border-style:none;'>" ) ;
      out.println ( "<tr><th>" + (LOGIN_ROLE.getDescription())
        + "</th><th>School</th><th>Competition</th></tr>" ) ;
      out.println ( "<tr><td>" +  (LOGIN_USER.getFirstName() + " " + LOGIN_USER.getLastName())
        + "</td><td>" + ((associatedSchool == null) ? ("Not Applicable") : (associatedSchool.getName()))
        + "</td><td>" + (COMPETITION.getName()) + "</td></tr>" ) ;
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

      // handle case where system administrator is logged in ...
      if ( ! LOGIN_ROLE.getDescription().equals("Administrator") )
        {
        out.println ( "    <form>" ) ;
        out.println ( "      <div>" ) ;
        out.println ( "        <fieldset>" ) ;
        out.println ( "          <legend>&nbsp;LEGEND&nbsp;</legend>" ) ;

        ResultSet rs = null ;
        out.print ( "<table>\n" ) ;
        sqlString = "SELECT * FROM Math_Relays.Student__Student_School(" + LOGIN_USER.getID() + "," + associatedSchool.getID() + ") " ;
        rs = sqlStatement.executeQuery ( sqlString ) ;
        rsmd = rs.getMetaData();
        out.print ( "<tr>\n" ) ;
        for ( int col = 1 ; col <= rsmd.getColumnCount() ; col++ )
          out.print( "<th>" + rsmd.getColumnName(col) + "<br>" + rsmd.getColumnTypeName(col)
                     + " (" + rsmd.getColumnType(col) + ")" + "</th>" ) ;
        out.print ( "</tr>\n" ) ;
        while ( rs.next() )
          {
          out.print ( "<tr>\n" ) ;
          for ( int col = 1 ; col <= rsmd.getColumnCount() ; col++ )
            out.print( "<td>" + rs.getString(col) + "</td>" ) ;
          out.print ( "</tr>\n" ) ;
          } // end while
        if ( rs != null ) { rs.close(); rs = null; }
        out.print ( "</table>\n" ) ;
        out.println ( "        </fieldset>" ) ;
        out.println ( "      </div>" ) ;
        out.println ( "    </form><hr>" ) ;
        } // end if

      out.print ( "    <form method='POST' action='TableMaintenance'>\n" ) ;
      out.print ( "      <input type='hidden' name='Table' value='" + TABLENAME + "'>\n" ) ;
      out.print ( "      <input type='hidden' id='hiddenInput' name='htmlTable' value=''>\n" ) ;
      out.print ( "      <div>\n" ) ;
      out.print ( "        <fieldset>\n" ) ;
      out.print ( "          <legend>&nbsp;Math Relays " + TABLENAME + "&nbsp;</legend>\n" ) ;
      out.print ( "          <table id='mathRelaysTable'>\n" ) ;

      sqlString = "SELECT * FROM Math_Relays." + TABLENAME + " " ;
      // handle case where logged in user is not a system administrator ...
      if ( ! LOGIN_ROLE.getDescription().equals("Administrator") )
        {
        String whereClause = "WHERE (" ;
        String preIN = null ;
        String postIN = null ;
        if (TABLENAME.equals( "STUDENT" ))
          { preIN = "ID"; postIN = "Student_ID" ; }
        else if (TABLENAME.equals( "STUDENT_SCHOOL" ))
          { preIN = "(Competition_ID,School_ID)"; postIN = "Competition_ID, School_ID" ; }
        else if (TABLENAME.equals( "STUDENT_TEAM" ))
          { preIN = "Student_ID"; postIN = "Student_ID" ; }
        else if (TABLENAME.equals( "TEAM" ))
          { preIN = "ID"; postIN = "Team_ID" ; }
        else if (TABLENAME.equals( "TEAM_EVENT" ))
          { preIN = "Team_ID"; postIN = "Team_ID" ; }
        whereClause += preIN + " IN (SELECT " + postIN ;
        whereClause += " FROM Math_Relays.Sponsor_to_All(" + LOGIN_USER.getID() + "))) " ;
        sqlString += whereClause ;
        } // end if not an Administrator

      tableRows  =  sqlStatement.executeQuery ( sqlString ) ;
      rsmd = tableRows.getMetaData() ;
      tableHeaderRow = "            <tr>\n              <th>&nbsp;Action&nbsp;</th>\n" ;
      templateRow = "<tr id='templateRow' class='not_displayed'>\n"
        +  "<td><button type='button' class='deleteButton'>&nbsp;Delete&nbsp;</button></td>\n" ;
      for ( int i = 1 ; i <= rsmd.getColumnCount() ; i ++ )
        {
        String colName = rsmd.getColumnName(i) ;
        boolean isPrimaryKey = pkMap.containsValue(colName) ;
        boolean isForeignKey = fkMap.containsKey(colName) ;
        tableHeaderRow += ( "              "
          +  "<th class='dataColumnHeader' "
          +  "data-column-number='" + i + "' "
          +  "data-column-name='" + rsmd.getColumnName(i) + "' "
          +  "data-column-type-name='" + rsmd.getColumnTypeName(i) + "' "
          +  "data-column-type='" + rsmd.getColumnType(i) + "' "
          +  (isPrimaryKey?"data-primary-key ":"")
          +  (isForeignKey?"data-foreign-key ":"") ) ;
        if ( isForeignKey )
          {
          tableHeaderRow += ( "data-foreign-key-value='[" ) ;
          Iterator<String> iter = fkMap.get(colName).iterator() ;
          boolean isFirst = true ;
          while ( iter.hasNext() )
            {
            if ( ! isFirst )  tableHeaderRow += (",") ;
            tableHeaderRow += ( "\"" + iter.next() + "\"" ) ;
            isFirst = false ;
            } // end while
          tableHeaderRow += ( "]' " ) ;
          } // end if
        tableHeaderRow += ( (rsmd.isAutoIncrement(i)?"data-auto-increment ":"")
          +  ">&nbsp;" + rsmd.getColumnLabel(i) + "&nbsp;</th>\n" ) ;
        String datum = "" ;
        if ( isForeignKey && (fkMap.get(colName).size() > 0) )
          datum = fkMap.get(colName).get(0) ;
        templateRow += tableDatum ( colName, rsmd.getColumnType(i), datum, isPrimaryKey, isForeignKey, rsmd.isAutoIncrement(i),
                                    fkMap.get(colName), rsmd.getColumnType(i), true ) ;
        } // end for loop
      tableHeaderRow += "            </tr>\n" ;
      templateRow += "            </tr>\n" ;
      out.print ( tableHeaderRow ) ;
      out.print ( templateRow ) ;

      columnMap  =  new HashMap<String,Element> () ;
      String tableWithHeader = "<table>" + tableHeaderRow + "</table>" ;
      Document doc = Jsoup.parseBodyFragment ( tableWithHeader ) ;
      Elements  dataColumnHeaders  =  doc.select("th.dataColumnHeader") ;
      Iterator<Element> iter = dataColumnHeaders.iterator();
      while ( iter.hasNext() )
        {
        Element dataColumnHeader = iter.next() ;
        String columnName = dataColumnHeader.attr("data-column-name") ;
        columnMap.put(columnName,dataColumnHeader) ;
        } // end while

      while ( tableRows.next() )
        out.print ( tableRow ( tableRows, rsmd, columnMap, fkMap ) ) ;

      out.print ( "          </table>\n" ) ;
      out.print ( "          <hr>\n" ) ;
      out.print ( "          <input id='insertButton' type='submit' style='width:100%;'\n" ) ;
      out.print ( "                 value='Insert New " + TABLENAME + "'>\n" ) ;
      out.print ( "          <hr>\n" ) ;
      out.print ( "          <input id='submitButton' type='submit' style='width:100%;'\n" ) ;
      out.print ( "                 value='Submit Changes to FHSU'>\n" ) ;
      out.print ( "        </fieldset>\n" ) ;
      out.print ( "      </div>\n" ) ;
      out.print ( "    </form>\n" ) ;
      // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
      out.print ( "  <hr></body>\n" ) ;
      out.print ( "</html>\n" ) ;
      out.flush();
      out.close() ;
      } // end try block
    catch ( URISyntaxException use )
      { use.printStackTrace() ; }
    catch ( ClassNotFoundException cnfe )
      { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )
      { sqle.printStackTrace() ; }
    finally
      {
      try
        {
        if ( tableRows         !=  null )  tableRows.close() ;
        if ( importedKeys      !=  null )  importedKeys.close() ;
        if ( foreignKeyValues  !=  null )  foreignKeyValues.close() ;
        if ( primaryKeys       !=  null )  primaryKeys.close() ;
        if ( sqlStatement      !=  null )  sqlStatement.close() ;
        if ( dbConn            !=  null )  dbConn.close() ;
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block

    } // end doPost method

  // ---------------------------------------------------------------------------------------------------

  private String tableRow
    (  ResultSet                 tableRows,
       ResultSetMetaData         rsmd,
       Map<String,Element>       columnMap,
       Map<String,List<String>>  fkMap  )
      throws SQLException
    {
    String row = "            <tr class='dataRow'>\n"
      + "<td><button type='button' class='deleteButton'>&nbsp;Delete&nbsp;</button></td>\n" ;
    for ( int i = 1 ; i <= rsmd.getColumnCount() ; i ++ )
      {
      String colName = rsmd.getColumnName(i) ;
      Element dataColumnHeader = columnMap.get(colName) ;
      Map<String,String> dataset = dataColumnHeader.dataset() ;
      boolean isPrimaryKey = (null != dataset.get("primary-key")) ;
      boolean isForeignKey = (null != dataset.get("foreign-key")) ;
      String datum = tableRows.getString(i) ;
      int colType = Integer.parseInt(dataset.get("column-type")) ;
      row += tableDatum ( dataset.get("column-name"), colType, datum, isPrimaryKey, isForeignKey, rsmd.isAutoIncrement(i), fkMap.get(colName), rsmd.getColumnType(i), false ) ;
      } // end for i loop
    row += ( "            </tr>\n" ) ;
    return  row ;
    } // end tableRow

  // ---------------------------------------------------------------------------------------------------

  // tableDatum is an auxiliary function a particular datum to be displayed on the web page
  private String tableDatum
    (  String columnName, int columnType, String datum, boolean isPrimaryKey, boolean isForeignKey, boolean isAutoIncrement, List<String> fkOptions, int sqlColType, boolean isTemplateRow  )
    {
    String tableDatum = "<td" + (isPrimaryKey?" data-primary-key ":"") + (isAutoIncrement?" data-auto-increment ":"") + " data-column-name='" + columnName + "' data-column-type='" + columnType + "'>" ;
    if ( isForeignKey )
      {
      tableDatum += "<select" + ( (isPrimaryKey && ! isTemplateRow) ? (" disabled") : ("") ) + ">" ;
      Iterator<String> optionIter = fkOptions.iterator() ;
      while ( optionIter.hasNext() )
        {
        String option = optionIter.next() ;
        String selected = ( (option.equals(datum)) ? (" selected") : ("") ) ;
        tableDatum += "<option value='" + option + "'" + selected + ">" + option + "</option>" ;
        } // end while
      tableDatum += "</select>\n" ;
      } // end if
    else // not a foreign key
      {
      String readStatus = ( (isAutoIncrement || (isPrimaryKey && ! isTemplateRow)) ? (" readonly") : ("") ) ;
      switch ( sqlColType )
        {
        case java.sql.Types.CHAR :
        case java.sql.Types.VARCHAR :
          tableDatum += "<input type='text' value='" + datum + "'" + readStatus + ">" ;
          break ;
        case java.sql.Types.DATE :
          tableDatum += "<input type='date' value='" + datum + "'" + readStatus + ">" ;
          break ;
        case java.sql.Types.INTEGER :
          tableDatum += "<input type='number' value='" + datum + "'" + readStatus + ">" ;
          break ;
        default :
          tableDatum += datum ;
          break ;
        } // end switch
      } // end else
    tableDatum += "</td>\n" ;
    return  tableDatum ;
    } // end tableDatum

  // ---------------------------------------------------------------------------------------------------

  } // end TableMaintenanceServlet class
