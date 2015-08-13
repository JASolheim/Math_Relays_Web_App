/*
 *  File: Role.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains a class from which objects representing specific roles of
 *  system users can be created.  For example, one type of role could be
 *  "System Administrator" (such as the FHSU Math Dept secretary),
 *  while another role might be "Faculty Sponsor" (i.e., a high school teacher
 *  who registers her or his students for various Math Relays events.
 */

package servlet;

// ------------------------------------------------------------------------------------ //

class Role
  {
  private  final  int     ID ;
  private  final  String  description ;

  Role  (  final int ID,  final String description  )
    {
    this.ID = ID ;
    this.description = description ;
	} // end constructor

  public  int     getID()           {  return  this.ID ;           }
  public  String  getDescription()  {  return  this.description ;  }
  } // end Role

// ------------------------------------------------------------------------------------ //
