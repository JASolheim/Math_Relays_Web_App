/*
 *  File: SysUser.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains a class from which specific system users can be created.
 */

package servlet;

// ------------------------------------------------------------------------------------ //

class SysUser
  {
  private  final  int     ID ;
  private  final  int     roleId ;
  private  final  String  userName ;
  private  final  String  password ;
  private  final  String  firstName ;
  private  final  String  lastName ;
  private  final  String  email ;
  private  final  String  phone ;

  SysUser
    (  final int ID,           final int    roleId,
       final String userName,  final String password,
       final String firstName, final String lastName,
       final String email,     final String phone  )
    {
    this.ID = ID ;
    this.roleId = roleId ;
    this.userName = userName ;
    this.password = password ;
    this.firstName = firstName ;
    this.lastName = lastName ;
    this.email = email ;
    this.phone = phone ;
	} // end constructor

  public  int     getID()           {  return  this.ID ;         }
  public  int     getRoleId()       {  return  this.roleId ;  }
  public  String  getUserName()     {  return  this.userName ;   }
  public  String  getFirstName()    {  return  this.firstName ;  }
  public  String  getLastName()     {  return  this.lastName ;   }
  public  String  getEMail()        {  return  this.email ;      }
  public  String  getPhone()        {  return  this.phone ;      }
  } // end SysUser

// ------------------------------------------------------------------------------------ //
