/*
 *  File: Math_Relays.js    Author: Jeffery A. Solheim    Date: August, 2014
 *
 *  This file contains definitions of JavaScript functions that serve as event handlers.
 */

// ----------------------------------------------------------------------------------------------------- //
function teamSelectChangeHandler ( sourceSelect )
  {
  var $sourceSelect = $(sourceSelect) ;
  var selectedVal = $sourceSelect.find('option:selected').val() ;
  var eventId = $sourceSelect.closest('td[data-event-id]').attr('data-event-id') ;
  var $siblingSelect = $sourceSelect.closest('tr.dataRow')
                                    .find('>td[data-event-id=\'' + eventId + '\']>select.eventgrade')
                                    .first() ;
  if ( selectedVal === '' )
    {
    $siblingSelect.fadeTo ( 'slow', 0.0, function() { $siblingSelect.css('visibility','hidden'); } ) ;
    } // end if
  else // selectedVal should be either I, A, or B
    {
    $siblingSelect.css('visibility','visible') ;
    $siblingSelect.fadeTo ( 'slow', 1.0 ) ;
    } // end else
  } // end teamSelectChangeHandler
// ----------------------------------------------------------------------------------------------------- //
function validateFormData ( form )
  {
  var okToSubmit = true ;

  // ---- confirm that each student is registered for at least four events ----
  $.each ( $('tr.dataRow').not('.deleted'),
           function ( outerIndex, outerValue )
             {
             var studentName = $(this).find('td[data-column-name=FirstName]>input').val() + ' '
                             + $(this).find('td[data-column-name=LastName]>input').val() ;
             var eventCount = 0 ;
             $.each ( $(this).find('select.eventteam option:selected'),
               function ( innerIndex, innerValue )
                 {
                 if ( $(this).text().length > 0 )
                   eventCount ++ ;
                 } ) ;
             okToSubmit = (okToSubmit && (eventCount >= 4)) ;
             if ( eventCount < 4 )
               window.alert ( 'Every student must register for at least 4 events.\n' + studentName + ' is registered for only ' + eventCount + ' events.' ) ;
             } ) ;
  // --------------------------------------------------------------------------

  // ---------- confirm a maximum 6 individuals per school per event ----------
  // ---------  and at most one team per grade level in each event  -----------
  // ---------------------- and 2 or 3 students per team ----------------------
  var iCount = { } ;
  var team = { } ;

  $.each ( $('tr.dataRow').not('.deleted').find('>td[data-event-id]>select.eventteam option:selected'),
           function ( index, value )
             {
             var eventId = $(this).closest('td[data-event-id]').attr('data-event-id') ;
             var grouping = $(this).text() ;
             if ( grouping === 'I' )
               {
               if ( typeof iCount[eventId] === 'undefined' )  iCount[eventId] = 1 ;
               else                                           iCount[eventId] = iCount[eventId] + 1 ;
               } // end if grouping is I
             if ( (grouping === 'A') || (grouping === 'B') )
               {
               if ( typeof team[eventId] === 'undefined' )
                 team[eventId] = { } ;
               if ( typeof team[eventId][grouping] === 'undefined' )
                 team[eventId][grouping] = { } ;

               var $currentRow = $(this).closest('tr.dataRow') ;
               var gradeValue = $currentRow.find('td[data-event-id=' + eventId + ']>select.eventgrade option:selected').first().val() ;
               var eventGrade = parseInt(gradeValue) ;

               if (    (typeof team[eventId][grouping]['gradeLevel'] === 'undefined')
                    || (eventGrade > team[eventId][grouping]['gradeLevel']) )
                 team[eventId][grouping]['gradeLevel'] = eventGrade ;

               if ( typeof team[eventId][grouping]['members'] === 'undefined' )
                 team[eventId][grouping]['members'] = 1 ; // set number of members 1
               else
                 team[eventId][grouping]['members'] = team[eventId][grouping]['members'] + 1 ;

               // e.g., team[1001]['A']['gradeLevel'] === 12 (team 'A' in event 1001 competing at grade level 12)
               // e.g., team[1001]['A']['members'] === 3 (3 members on team 'A' in event 1001)
               } // end if grouping is A or B
             } ) ; // end each eventteam selected option

  $.each ( iCount,
           function ( index, value )
             {
             okToSubmit = (okToSubmit && (value <= 6)) ;
             if ( value > 6 )
               window.alert ( 'Not more than 6 students may compete as individuals per event.\nYou are attempting to register ' + value + ' individual contestants for event # ' + index + '.' ) ;
             } ) ;

  // ---------  and at most one team per grade level in each event  -----------
  // ---------------------- and 2 or 3 students per team ----------------------
  $.each ( team, function ( eventId, val1 )
    {
    console.info ( 'team[' + eventId + '] = ' + val1 ) ;
    $.each ( val1, function ( grouping, val2 )
      {
      console.info ( '\tteam[' + eventId + '][' + grouping + '] = ' + val2 ) ;
      $.each ( val2, function ( dataClass, val3 ) // dataClass is either 'gradeLevel' or 'members'
        {
        if ( (dataClass === 'gradeLevel') && (grouping === 'B') )
          {
          if ( typeof team[eventId]['A'] !== 'undefined' )
            {
            var gradeLevelA = team[eventId]['A']['gradeLevel'] ;
            var gradeLevelB = team[eventId]['B']['gradeLevel'] ;
            okToSubmit = (okToSubmit && (gradeLevelA !== gradeLevelB)) ;
            if ( gradeLevelA === gradeLevelB )
              window.alert( 'Each school is permitted at most one team per grade level in each event.\n\nIn event # '
                + eventId + ', you have two teams both competing at grade level ' + gradeLevelA + '.' ) ;
            } // end if
          } // end if
        else if ( dataClass === 'members' )
          {
          var numMembers = team[eventId][grouping]['members'] ;
          // var numMembers = Object.keys(team[eventId][grouping]['members']).length ;
          okToSubmit = (okToSubmit && ((2 <= numMembers) && (numMembers <= 3))) ;
          if ( (numMembers < 2) || (numMembers > 3) )
            window.alert('Each team must be comprised of 2 or 3 students.\n\nIn event # ' + eventId
              + ', team ' + grouping + ' has ' + numMembers + ' members.') ;
          } // end else if

        console.info ( '\t\tteam[' + eventId + '][' + grouping + '][' + dataClass + '] = ' + val3 ) ;
        $.each ( val3, function ( studentId, val4 )
          {
          console.info ( '\t\t\tteam[' + eventId + '][' + grouping + '][' + dataClass + '][' + studentId + '] = ' + val4 ) ;
          } ) ;
        } ) ;
      } ) ;
    } ) ;
  // --------------------------------------------------------------------------

  if ( ! okToSubmit )
    return false ;
  else // okToSubmit
    {
    var tableStr = (new XMLSerializer())
                   .serializeToString(document.getElementById('mathRelaysTable'));
    $('#hiddenInput').val(tableStr) ;
    return true ;
    } // end else
  } // end validateFormData
// ----------------------------------------------------------------------------------------------------- //
function deleteButtonClickHandler ( button )
  {
  var $trElem = $(button).closest('tr') ;

  // if was previously inserted, then just remove this row & return ...
  if ( $trElem.hasClass('inserted') )
    $trElem.remove() ;
  else // $trElem has not been inserted during this editing session
    {
    $trElem.toggleClass('deleted') ;
    if ( $trElem.hasClass('deleted') )
      $trElem.find('.deleteButton').text('Restore') ;
    else
      $trElem.find('.deleteButton').text('Delete') ;
    } // end else
  } // end deleteButtonClickHandler
// ----------------------------------------------------------------------------------------------------- //
function updateSelectedAttribute ( target )
  {
  var $target = $(target) ;
  var selectedIndex = $target.prop('selectedIndex') ;
  $.each ( $target.find('option'),
           function ( index, value )
             {
             if ( index === selectedIndex )  $(this).attr('selected','selected') ;
             else                            $(this).removeAttr('selected') ;
             } ) ;
  $target.closest('tr').addClass('updated') ;
  } // end updateSelectedAttribute
// ----------------------------------------------------------------------------------------------------- //
function insertButtonClickHandler()
  {
  var $templateRow = $('#templateRow') ;
  var $trClone = $templateRow.clone() ;
  $trClone.removeAttr('id') ;
  $trClone.removeClass('not_displayed') ;
  $trClone.addClass('dataRow inserted') ;
  var $deleteButton = $trClone.find('.deleteButton') ;
  $deleteButton.on ( 'click', function(event)
    {
    event.preventDefault() ;
    deleteButtonClickHandler(this) ;
    } ) ;
  $trClone.find('select').on ( 'change',
    function(event)
      {
      event.preventDefault() ;
      var $target = $(this) ;
      var selectedIndex = $target.prop('selectedIndex') ;
      $.each ( $target.find('option'),
               function ( index, value )
                 {
                 if ( index === selectedIndex )  $(this).attr('selected','selected') ;
                 else                            $(this).removeAttr('selected') ;
                 } ) ;
	  } ) ;
  $trClone.find('input').on('change',
    function(event)
      {
      event.preventDefault() ;
      var $target = $(this) ;
      $target.attr('value',$target.val()) ;
      } ) ;
  $trClone.find('select.grade').on ( 'change', function(event)
    {
    event.preventDefault() ;
    updateGradeOptions(this) ;
    } ) ;
  // -------------------------------------------- //
  $trClone.find('>td[data-event-id]>select.eventteam').change (
    function ( event )
      {
      event.preventDefault() ;
      teamSelectChangeHandler ( this ) ;
      } ) ;
  // -------------------------------------------- //

  var $trParent = $templateRow.parent() ;
  $trClone.prependTo( $trParent ) ;
  updateGradeOptions ( $trClone.find('td[data-table-name=Student_School][data-column-name=Grade_ID]>select.grade').first() ) ;
  } // end insertButtonClickHandler
// ----------------------------------------------------------------------------------------------------- //
function updateGradeOptions ( sourceSelect )
  {
  var $sourceSelect = $(sourceSelect) ;
  var sourceGrade = parseInt( $sourceSelect.val() ) ;
  var $trElem = $sourceSelect.closest('tr') ;
  var $targetSelect = $trElem.find('select.eventgrade') ;
  $.each ( $targetSelect, function ( index, targetSelect )
    {
    var $targetSelect = $(targetSelect) ;
    var minGrade = parseInt ( $targetSelect.closest('td[data-min-grade]').attr('data-min-grade') ) ;
    var maxGrade = parseInt ( $targetSelect.closest('td[data-max-grade]').attr('data-max-grade') ) ;
    $targetSelect.empty() ;
    for ( var i =  ( (minGrade > sourceGrade) ? minGrade : sourceGrade ) ;
              i <= ( (maxGrade < 12) ? maxGrade : 12 ) ; i ++ )
      {
      var selectString = ( ((i === sourceGrade) || ((minGrade > sourceGrade) && (i === minGrade))) ? (' selected') : ('') ) ;
      $targetSelect.append('<option value=\'' + i + '\'' + selectString + '>' + i + '</option>') ;
      } // end for i

    } ) ;
  } // end updateGradeOptions
// ----------------------------------------------------------------------------------------------------- //
$(document).ready ( function() {
  // -------------------------------------------- //
  $('.deleteButton').on ( 'click',
    function(event)
      {
      event.preventDefault() ;
      deleteButtonClickHandler(this) ;
      } ) ;
  // -------------------------------------------- //
  $('#insertButton').on ( 'click',
    function(event)
      {
      event.preventDefault() ;
      insertButtonClickHandler(this) ;
      } ) ;
  // -------------------------------------------- //
  $('tr.dataRow').find('select.grade').on ( 'change',
    function(event)
      {
      event.preventDefault() ;
      updateGradeOptions(this) ;
      } ) ;
  // -------------------------------------------- //
  $('tr.dataRow>td[data-event-id]>select.eventteam').change (
    function ( event )
      {
      event.preventDefault() ;
      teamSelectChangeHandler ( this ) ;
      } ) ;
  // -------------------------------------------- //
  $('tr.dataRow').find('select').on ( 'change',
    function(event)
      {
      event.preventDefault() ;
      updateSelectedAttribute(this) ;
      } ) ;
  // -------------------------------------------- //
  $('tr.dataRow').find('input').on('change',
    function(event)
      {
      event.preventDefault() ;
      var $target = $(this) ;
      $target.attr('value',$target.val()) ;
      $target.closest('tr').addClass('updated') ;
      } ) ;
  // -------------------------------------------- //
  $('td>select.grade').attr( 'title',
    '\u25B8  The grade in which this student is currently enrolled.' ) ;
  $('td>select.eventteam').attr( 'title',
    '\u25B8  Blank if not competing in this event\x0D\u25B8  I  if competing as an Individual\x0D\u25B8  A  if competing as a member of team A\x0D\u25B8  B  if competing as a member of team B' ) ;
  $('td>select.eventgrade').attr( 'title',
    '\u25B8  A student may compete in different events at different grade levels.\x0D\u25B8  A student may compete at or above, but not below, her or his current grade level.' ) ;
  // -------------------------------------------- //
} ) ;
// ----------------------------------------------------------------------------------------------------- //
