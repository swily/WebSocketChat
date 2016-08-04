var connection = null;
var userList = null;
var userMe = null;

var host = 'localhost:8080';

function joinChat()
{
  var username = $('#username').val();
  userMe = username;
  
  var useWebSockets = $('#useWebSockets').prop('checked');
  
  $('#joinButton').prop('disabled', true);
  $('#leaveButton').prop('disabled', false);
  $('#username').prop('disabled', true);
  $('#useWebSockets').prop('disabled', true);
  
  if (useWebSockets) {
    setupWebSockets(username);
  }
  else {
    setupLongPolling(username);
  }
}

function setupWebSockets(username)
{
  connection = new WebSocket('ws://' + host + '/chat');

  connection.onopen = function() {
    console.log('opened websocket connection');
    
    var msg = new Message("join", username);
    var json = JSON.stringify(msg);
    
    console.log('joining chat: ' + json);
    connection.send(json);
  };
  
  connection.onclose = function(e) {
    console.log('closed websocket connection');
    
    if (connection != this) {
      return;
    }

    hideRoom();
  };
  
  connection.onerror = function(e) {
    if (connection != this) {
      return;
    }
    
    hideRoom();
  };
  
  connection.onmessage = function(value) {
    console.log("received message: " + value.data);
    
    if (connection != this) {
      return;
    }

    var msg = JSON.parse(value.data);
    
    onReceive(msg);
  };
}

function onReceive(msg)
{
  if (! isConnected()) {
    return;
  }

	switch (msg.type) {
		case "list":
			updateList(msg.users);
			break;
		
		case "join":
			updateListJoin(msg.user);
			break;
		
		case "leave":
			updateListLeave(msg.user);
			break;
		
		case "message":
			onChatMessage(msg.user, msg.value);
			break;
	}
}

function setupLongPolling(username)
{
  $.ajax({
    url: 'http://' + host + '/join?user=' + username
  }).done(function(data) {
    updateList(data);
    
    poll(username);
  }).fail(function() {
    hideRoom();
  });
}

function poll(username, lastMessageId)
{
  if (! isConnected()) {
    return;
  }

  var idQuery = '';
  if (lastMessageId != null) {
    idQuery = 'id=' + lastMessageId;
  }
  
  $.ajax({
    url: 'http://' + host + '/getMessages?user=' + username + '&' + idQuery
  }).done(function(list) {
    if (list.length > 0 && lastMessageId == null) {
      lastMessageId = 0;
    }
    
    for (var i = 0; i < list.length; i++) {
      var msg = list[i];
      lastMessageId = Math.max(lastMessageId, msg.id);
      onReceive(msg);
    }
    
    poll(username, lastMessageId);
  }).fail(function() {
    hideRoom();
  });
}

function hideRoom()
{
  connection = null;
  userList = null;
  userMe = null;

  $('#joinButton').prop('disabled', false);
  $('#leaveButton').prop('disabled', true);
  $('#username').prop('disabled', false);
  $('#useWebSockets').prop('disabled', false);

  $('#room').hide();
  $('#messages').empty();
  $('#list').empty();
}

function leaveChat()
{
  var username = $("#username").val();
  var msg = new Message("leave", username);
  
  if (isConnected()) {
		if (connection != null) {
			var json = JSON.stringify(msg);
	
			console.log('leaving chat: ' + json);
			connection.send(json);
			connection.close();
		
			connection = null;
		}
		else {
			$.ajax({
				url: 'http://' + host + '/leave?user=' + username
			});
		}
  }
  
  hideRoom();
}

function sendMessage()
{
  var message = $('#message').val();
  var username = $('#username').val();
  
  $('#message').val('');
  
  var msg = new Message("message", username, message);
  
  if (connection != null) {
    var json = JSON.stringify(msg);
    console.log('sending message: ' + json);
    connection.send(json);
  }
  else {
    $.ajax({
      url: 'http://' + host + '/send?user=' + username + '&msg=' + message
    }).done(function(data) {
      
    });
  }
}

function updateList(list)
{
  userList = list;

  $('#list').empty();
  
  $.each(list, function(index, user) {
    addToList(user);
  });
  
  $('#room').show('slow');
}

function updateListJoin(user)
{
  onControlMessage(user + ' has joined the room');

  if (user == userMe) {
    return;
  }

  userList.push(user);
  
  addToList(user);
}

function addToList(user)
{
  var html = '<li id="user_' + user + '">' + user + '</li>';
  
  $('#list').append(html);
}

function updateListLeave(user)
{
  onControlMessage(user + ' has left the room');

  var index = userList.indexOf(user);
  
  if (index >= 0) {
    userList.splice(index, 1);
  }
  
  $('#user_' + user).remove();
}

function isConnected()
{
  return userMe != null;
}

function onChatMessage(user, msg)
{
  var html;
  
  if (user == userMe) {
    html = '<li class="my-message">' + user + ': ' + msg + '</li>';
  }
  else {
    html = '<li>' + user + ': ' + msg + '</li>';
  }
  
  addMessage(html);
}

function onControlMessage(msg)
{
  var html = '<li class="control-message">' + msg + '</li>';
  
  addMessage(html);
}

function addMessage(html)
{
  var e = $('#messages');
  
  e.append(html);
  e.scrollTop(e.prop("scrollHeight"));
}

var Message = function(type, user, value) {
  this.type = type;
  this.user = user;
  this.value = value;
}

$(document).ready(function(e) {
  $('#message').keypress(function(e) {
    e = e || window.event;
    var charCode = (typeof e.which == "number") ? e.which : e.keyCode;
    if (charCode == 13) {
      sendMessage();
    }
  });
  
  $(window).on("beforeunload", function() { 
    leaveChat();
  })
});
