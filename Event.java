/*
 *  File: Event.java      Author: Jeffery A. Solheim      Date: August, 2014
 *
 *  This file contains a class from which objects representing specific Math Relays
 *  events can be created.
 */

package servlet;

// ------------------------------------------------------------------------------------ //

class Event
  {
  private  final  int     ID ;
  private  final  int     competitionID ;
  private  final  String  name ;
  private  final  int     minGrade ;
  private  final  int     maxGrade ;

  Event
    (  final  int     ID ,
       final  int     competitionID ,
       final  String  name ,
       final  int     minGrade ,
       final  int     maxGrade  )
    {
    this.ID = ID ;
    this.competitionID = competitionID ;
    this.name = name ;
    this.minGrade = minGrade ;
    this.maxGrade  = maxGrade ;
	} // end constructor

  int     getID()             {  return  this.ID ;             }
  int     getCompetitionID()  {  return  this.competitionID ;  }
  String  getName()           {  return  this.name ;           }
  int     getMinGrade()       {  return  this.minGrade ;       }
  int     getMaxGrade()       {  return  this.maxGrade ;       }
  } // end Event

// ------------------------------------------------------------------------------------ //
