/*
 *  File: TableMaintenance.js    Author: Jeffery A. Solheim    Date: August, 2014
 *
 *  This file contains definitions of JavaScript functions that serve as event handlers.
 *
 *  Note: there might be sufficient similarity between the functions defined in
 *  this file and those defined in Math_Relays.js that the files might be
 *  combined.  This could be explored later as time permits.
 */

// --------------------------------------------------------------------------- //
function deleteButtonClickHandler ( button )
  {
  var $trElem = $(button).closest('tr') ;

  // if was previously inserted, then just remove this row & return ...
  if ( $trElem.hasClass('inserted') )
    {
    $trElem.remove() ;
    } // end if
  else // $trElem has not been inserted during this editing session
    {
    $trElem.toggleClass('deleted') ;
    if ( $trElem.hasClass('deleted') )
      $trElem.find('.deleteButton').text('Restore') ;
    else
      $trElem.find('.deleteButton').text('Delete') ;
    } // end else
  } // end deleteButtonClickHandler
// --------------------------------------------------------------------------- //
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
// --------------------------------------------------------------------------- //
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
  $trClone.find('select')
    .on ( 'change', function(event) { event.preventDefault() ;
                                      updateSelectedAttribute(this) ; } ) ;
  $trClone.find('input').on('change',
    function(event)
      {
      event.preventDefault() ;
      var $target = $(this) ;
      $target.attr('value',$target.val()) ;
      $target.closest('tr').addClass('updated') ;
      } ) ;
  var $trParent = $templateRow.parent() ;
  $trClone.appendTo( $trParent ) ;
  } // end insertButtonClickHandler
// --------------------------------------------------------------------------- //
$(document).ready ( function() {
  // -------------------------------------------- //
  $('#submitButton').on ( 'click', function( event ) {
    var tableStr = (new XMLSerializer())
                   .serializeToString(document.getElementById('mathRelaysTable'));
    $('#hiddenInput').val(tableStr) ;
  } ) ;
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
} ) ;
// --------------------------------------------------------------------------- //
