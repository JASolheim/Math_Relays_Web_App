-- 
-- -------------------------------------------------------------------------------------------------
-- File:  Create_Math_Relays_DB.SQL    Author: Jeffery A. Solheim     Date: October, 2014
-- 
-- This file contains a PostgreSQL script that, when run, creates the tables of the Math_Relays
-- schema.  The tables are described in the document ...\src\main\webapp\Database_Description.pdf.
-- -------------------------------------------------------------------------------------------------
-- 
DROP SCHEMA IF EXISTS Math_Relays CASCADE ;
CREATE SCHEMA Math_Relays ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Competition
  ( ID         SERIAL  PRIMARY KEY,
    CompDate   DATE    NOT NULL,
    Name       VARCHAR(45) ) ;
ALTER SEQUENCE Math_Relays.Competition_Id_Seq RESTART WITH 11 ;
Insert Into Math_Relays.Competition(CompDate,Name) Values('2014-11-13','2014 Math Relays');
SELECT * FROM Math_Relays.Competition ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Role
  ( ID           SERIAL       PRIMARY KEY,
    Description  VARCHAR(20)  NOT NULL ) ;
ALTER SEQUENCE Math_Relays.Role_Id_Seq RESTART WITH 11 ;
Insert Into Math_Relays.Role(Description) Values( 'Administrator' ) ;
Insert Into Math_Relays.Role(Description) Values( 'Faculty Sponsor'  ) ;
SELECT * FROM Math_Relays.Role ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.SysUser
  ( ID         SERIAL       PRIMARY KEY,
    Role_ID    INT          NOT NULL
                            REFERENCES Math_Relays.Role(ID),
    UserName   VARCHAR(15)  UNIQUE
                            CHECK ((UserName IS NOT NULL) AND (LENGTH(UserName) >= 3)),
    Password   VARCHAR(15)  CHECK ((Password IS NOT NULL) AND (LENGTH(Password) >= 6)),
    LastName   VARCHAR(20)  NOT NULL,
    FirstName  VARCHAR(20)  NOT NULL,
    EMail      VARCHAR(40),
    Phone      VARCHAR(16) ) ;
ALTER SEQUENCE Math_Relays.SysUser_Id_Seq RESTART WITH 1001 ;
-- System Administrators ... ----------------------------------------------------------------
Insert Into Math_Relays.SysUser(Role_ID,UserName,Password,FirstName,LastName,EMail,Phone)
       Values(11,'LTerry','LeePass','Lee','Terry','LeeT@Somewhere.com','123-555-1212');
-- Faculty Sponsors ... ---------------------------------------------------------------------
Insert Into Math_Relays.SysUser(Role_ID,UserName,Password,FirstName,LastName,EMail,Phone)
       Values(12,'JoeTeacher','JoePass','Joseph','Teach','JTeach@Somewhere.com','123-555-1212');
-- 
SELECT * FROM Math_Relays.SysUser ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Classification
  (  Code         CHAR(2)   PRIMARY KEY
                            CHECK ( LENGTH(Code) >= 1 ),
     Description  VARCHAR(20)  ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '1A', 'KSHSAA Class 1A' ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '2A', 'KSHSAA Class 2A' ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '3A', 'KSHSAA Class 3A' ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '4A', 'KSHSAA Class 4A' ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '5A', 'KSHSAA Class 5A' ) ;
Insert Into Math_Relays.Classification(Code,Description) Values( '6A', 'KSHSAA Class 6A' ) ;
SELECT * FROM Math_Relays.Classification ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.School
  (  ID                SERIAL  PRIMARY KEY,
     Name              VARCHAR(40) NOT NULL,
     Address           VARCHAR(40),
     City              VARCHAR(25),
     State             CHAR(2),
     PostalCode        VARCHAR(10),
     WebAddress        VARCHAR(40),
     EMail             VARCHAR(40),
     Phone             VARCHAR(16)  ) ;
ALTER SEQUENCE Math_Relays.School_Id_Seq RESTART WITH 101 ;
-- High Schools ... -------------------------------------------------------------------------
Insert Into Math_Relays.School(Name,Address,City,State,PostalCode,WebAddress,EMail,Phone)
       Values('Frankfort','123 Maple Street','Frankfort','KY','40103',NULL,NULL,NULL);
-- 
SELECT * FROM Math_Relays.School ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.School_Classification
  (  Competition_ID  INT      NOT NULL
                              REFERENCES Math_Relays.Competition(ID),
     School_ID       INT      NOT NULL
                              REFERENCES Math_Relays.School(ID),
     Classification_Code      CHAR(2)  NOT NULL
                              REFERENCES Math_Relays.Classification(Code),
     PRIMARY KEY ( Competition_ID, School_ID )  ) ;
-- School Classifications ... ---------------------------------------------------------------
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 101, '4A' ) ;
-- 
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 102, '3A' ) ;
-- 
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 103, '1A' ) ;
-- 
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 104, '2A' ) ;
-- 
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 105, '2A' ) ;
-- 
Insert Into Math_Relays.School_Classification ( Competition_ID, School_ID, Classification_Code )
       Values ( 11, 106, '1A' ) ;
-- 
SELECT * FROM Math_Relays.School_Classification ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Sponsor_School
  (  Competition_ID  INT  NOT NULL
                          REFERENCES Math_Relays.Competition(ID),
     Sponsor_ID      INT  NOT NULL
                          REFERENCES Math_Relays.SysUser(ID),
     School_ID       INT  NOT NULL
                          REFERENCES Math_Relays.School(ID),
     PRIMARY KEY ( Competition_ID, Sponsor_ID ),
     UNIQUE ( Competition_ID, School_ID )  ) ;
     -- this UNIQUE constraint ensures that there will be at most one faculty sponsor
     -- for any given Competition & School
COMMENT ON TABLE Math_Relays.Sponsor_School IS 
  'Faculty Sponsors and the Schools they Represent' ;
-- Faculty Sponsors and their Schools ... ---------------------------------------------------
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1006,101);
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1007,102);
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1008,103);
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1009,104);
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1010,105);
Insert Into Math_Relays.Sponsor_School(Competition_ID,Sponsor_ID,School_ID) Values(11,1011,106);
-- 
SELECT * FROM Math_Relays.Sponsor_School ;
SELECT      SU.UserName, SU.Role_ID, C.CompDate, S.Name
FROM        Math_Relays.SysUser AS SU
INNER JOIN  Math_Relays.Sponsor_School AS SS  ON  SU.ID = SS.Sponsor_ID
INNER JOIN  Math_Relays.School AS S           ON  SS.School_ID = S.ID
INNER JOIN  Math_Relays.Competition AS C      ON  SS.Competition_ID = C.ID ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Student
  (  ID             SERIAL       PRIMARY KEY,
     LastName       VARCHAR(20)  NOT NULL,
     FirstName      VARCHAR(20)  NOT NULL  ) ;
ALTER SEQUENCE Math_Relays.Student_Id_Seq RESTART WITH 10001 ;
SELECT * FROM Math_Relays.Student ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Grade
  (  ID           INT          NOT NULL,
     Description  VARCHAR(20)  NOT NULL,
     PRIMARY KEY ( ID )  ) ;
Insert Into Math_Relays.Grade(ID,Description) Values(  9,'Freshman'  ) ;
Insert Into Math_Relays.Grade(ID,Description) Values( 10,'Sophomore' ) ;
Insert Into Math_Relays.Grade(ID,Description) Values( 11,'Junior'    ) ;
Insert Into Math_Relays.Grade(ID,Description) Values( 12,'Senior'    ) ;
SELECT * FROM Math_Relays.Grade ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Student_School
  (  Competition_ID  INT  NOT NULL
                          REFERENCES Math_Relays.Competition(ID),
     Student_ID      INT  NOT NULL
                          REFERENCES Math_Relays.Student(ID)
                          ON UPDATE CASCADE
                          ON DELETE CASCADE,
     School_ID       INT  NOT NULL
                          REFERENCES Math_Relays.School(ID),
     Grade_ID        INT  NOT NULL
                          REFERENCES Math_Relays.Grade(ID),
     PRIMARY KEY ( Competition_ID, Student_ID )  ) ;
SELECT * FROM Math_Relays.Student_School ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Event
  (  ID              SERIAL       PRIMARY KEY,
     Competition_ID  INT          NOT NULL
                                  REFERENCES Math_Relays.Competition(ID),
     Name            VARCHAR(45)  NOT NULL,
     MinGrade        INT          NOT NULL DEFAULT 9
                                  REFERENCES Math_Relays.Grade(ID),
     MaxGrade        INT          NOT NULL DEFAULT 12
                                  REFERENCES Math_Relays.Grade(ID),
     UNIQUE ( Competition_ID, Name )  ) ;
ALTER SEQUENCE Math_Relays.Event_Id_Seq RESTART WITH 1001 ;
Insert Into Math_Relays.Event(Competition_ID,Name) Values(11,'Using Number Sense');
Insert Into Math_Relays.Event(Competition_ID,Name,MinGrade) Values(11,'Trigonometry',11);
Insert Into Math_Relays.Event(Competition_ID,Name) Values(11,'Algebraic Manipulations');
Insert Into Math_Relays.Event(Competition_ID,Name) Values(11,'Informal Geometry, Measurement');
Insert Into Math_Relays.Event(Competition_ID,Name) Values(11,'Applications of Algebra, Arithmetic');
Insert Into Math_Relays.Event(Competition_ID,Name) Values(11,'Calculations with Calculators');
SELECT * FROM Math_Relays.Event ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Math_Relays.Student_Event
  (  Student_ID         INT  NOT NULL
                        REFERENCES Math_Relays.Student(ID)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
     Event_ID  INT      NOT NULL
                        REFERENCES Math_Relays.Event(ID)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
     Grade_ID  INT      NOT NULL
                        REFERENCES Math_Relays.Grade(ID),
     Grouping  CHAR(1)  NOT NULL
                        CHECK ((Grouping = 'I') OR (Grouping = 'A') OR (Grouping = 'B')),
     PRIMARY KEY ( Student_ID, Event_ID )  ) ;
-- -------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION Math_Relays.Student__Student_School
    ( School_ID_ARG INTEGER, Competition_ID_ARG INTEGER ) 
  RETURNS TABLE
            (  ID INTEGER, LastName TEXT, FirstName TEXT, 
               School_ID INTEGER, School_Name TEXT,
               Competition_ID INTEGER, CompDate DATE, Competition_Name TEXT, 
               Student_ID INTEGER, Grade_ID INTEGER 
                ) AS $$
    SELECT      Student.ID, Student.LastName, Student.FirstName, 
                Student_School.School_ID, School.Name, 
                Student_School.Competition_ID, Competition.CompDate, Competition.Name, 
				Student_School.Student_ID, Student_School.Grade_ID                 
    FROM        Math_Relays.Student_School
    INNER JOIN  Math_Relays.Student
    ON          (Student_School.Student_ID = Student.ID)
    INNER JOIN  Math_Relays.School
    ON          (Student_School.School_ID = School.ID)
    INNER JOIN  Math_Relays.Competition
    ON          (Student_School.Competition_ID = Competition.ID)
    WHERE       (  (Student_School.School_ID = School_ID_ARG) 
    AND            (Competition.ID = Competition_ID_ARG)  ) 
    ORDER BY    School.Name ASC,
                Competition.CompDate ASC,
                Competition.Name ASC,
                Student_School.Grade_ID ASC,
                Student.LastName ASC,
                Student.FirstName ASC,
                Student.ID ASC
$$  LANGUAGE SQL ;
SELECT      *
FROM        (SELECT 10 AS GR_ID) AS ALIAS_1
CROSS JOIN  (SELECT ID AS EV_ID
             FROM Math_Relays.Event
             WHERE Event.Competition_ID = 12
             ORDER BY Event.ID) AS ALIAS_2
CROSS JOIN   (SELECT 123 AS INDIV) AS ALIAS_3
CROSS JOIN   (SELECT 456 AS TEAM) AS ALIAS_4
 ;
SELECT      Event.Competition_ID AS Comp_ID,
            Event.ID AS Event_ID, Event.Name AS Event,
            Student_Event.Student_ID AS Student,
            Student_Event.Grade_ID AS Grade,
            Student_Event.Grouping AS Grouping
FROM        Math_Relays.Event
INNER JOIN  Math_Relays.Student_Event
ON          ( Event.ID = Student_Event.Event_ID )
INNER JOIN  Math_Relays.Student_School
ON          (     (Student_School.Competition_ID = Event.Competition_ID)
              AND (Student_School.Student_ID = Student_Event.Student_ID) )
WHERE       ( Event.Competition_ID = 12 )
AND         ( Student_School.School_ID = 101 )
;
SELECT      COUNT(*) AS Count
FROM        Math_Relays.Event
INNER JOIN  Math_Relays.Student_Event
ON          ( Event.ID = Student_Event.Event_ID )
INNER JOIN  Math_Relays.Student_School
ON          (     (Student_School.Competition_ID = Event.Competition_ID)
              AND (Student_School.Student_ID = Student_Event.Student_ID) )
WHERE       ( Event.Competition_ID = 12 )
AND         ( Student_School.School_ID = 101 )
AND         ( Event.ID IN ( 1002 ) )
AND         ( Student_Event.Grade_ID IN ( 11,12 ) )
AND         ( Student_Event.Grouping IN ( 'A','B' ) )
;
-- -------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION Math_Relays.Student_Count
  (  Competition_ID_ARG INTEGER,
     School_ID_ARG INTEGER,
     Event_ID_ARG INTEGER,
     MinGrade_ARG INTEGER,
     MaxGrade_ARG INTEGER,
     Min_Grouping_ARG CHAR(1),
     Max_Grouping_ARG CHAR(1)  )   RETURNS INTEGER AS $$
  DECLARE returnValue INTEGER;
  BEGIN
    SELECT      COUNT(*) INTO returnValue
    FROM        Math_Relays.Event
    INNER JOIN  Math_Relays.Student_Event
    ON          ( Event.ID = Student_Event.Event_ID )
    INNER JOIN  Math_Relays.Student_School
    ON          (     (Student_School.Competition_ID = Event.Competition_ID)
                  AND (Student_School.Student_ID = Student_Event.Student_ID) )
    WHERE       ( Event.Competition_ID = Competition_ID_ARG )
    AND         ( Student_School.School_ID = School_ID_ARG )
    AND         ( Event.ID = Event_ID_ARG )
    AND         ( Student_Event.Grade_ID >= MinGrade_ARG )
    AND         ( Student_Event.Grade_ID <= MaxGrade_ARG )
    AND         ( Student_Event.Grouping >= Min_Grouping_ARG )
    AND         ( Student_Event.Grouping <= Max_Grouping_ARG ) ;
    RETURN returnValue ;
    -- ----------------------------------------------
  END;
$$  LANGUAGE plpgsql;
-- -------------------------------------------------------------------------------------------------
