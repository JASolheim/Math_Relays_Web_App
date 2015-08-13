/*
 *  File: Competition.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains a class from which objects representing specific Math Relays
 *  competitions can be created.
 */

package servlet;

// ------------------------------------------------------------------------------------ //

import java.sql.Date ;

// ------------------------------------------------------------------------------------ //

class Competition
  {
  private  final  int     ID ;
  private  final  Date    compDate ;
  private  final  String  name ;

  Competition
    (  final int ID,  final Date compDate,  final String name  )
    {
    this.ID = ID ;
    this.compDate = compDate ;
    this.name = name ;
	} // end constructor

  int     getID()        {  return  this.ID ;        }
  Date    getCompDate()  {  return  this.compDate ;  }
  String  getName()      {  return  this.name ;      }
  } // end Competition

// ------------------------------------------------------------------------------------ //
