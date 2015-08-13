/*
 *  File: School.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains a class from which objects representing specific high
 *  schools participating in Math Relays can be created.
 */

package servlet;

// ------------------------------------------------------------------------------------ //

class School
  {
  private  final  int     ID ;
  private  final  String  name ;
  private  final  String  address ;
  private  final  String  city ;
  private  final  String  state ;
  private  final  String  postalCode ;
  private  final  String  webAddress ;
  private  final  String  eMail ;
  private  final  String  phone ;

  School
    (  final  int     ID,
       final  String  name,
       final  String  address,
       final  String  city,
       final  String  state,
       final  String  postalCode,
       final  String  webAddress,
       final  String  eMail,
       final  String  phone  )
    {
    this.ID = ID ;
    this.name = name ;
    this.address = address ;
    this.city = city ;
    this.state = state ;
    this.postalCode = postalCode ;
    this.webAddress = webAddress ;
    this.eMail = eMail ;
    this.phone = phone ;
	} // end constructor

  int     getID()          {  return  this.ID ;          }
  String  getName()        {  return  this.name ;        }
  String  getAddress()     {  return  this.address ;     }
  String  getCity()        {  return  this.city ;        }
  String  getState()       {  return  this.state ;       }
  String  getPostalCode()  {  return  this.postalCode ;  }
  String  getWebAddress()  {  return  this.webAddress ;  }
  String  getEMail()       {  return  this.eMail ;       }
  String  getPhone()       {  return  this.phone ;       }
  } // end School

// ------------------------------------------------------------------------------------ //
