/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.glines.socketio.examples.gwtchat.client;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.gwt.GWTSocketIOConnectionFactory;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

public class GWTChatClient implements EntryPoint, SocketIOConnection.SocketIOConnectionListener {

	SocketIOConnection socket;
	HTML htmlPanel;
	FlowPanel submitPanel;
	TextBox textBox;
	
	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();
		rootPanel.setSize("800", "300");
		
		htmlPanel = new HTML("Connecting...");
		htmlPanel.setStyleName("chat");
		rootPanel.add(htmlPanel);
		
		submitPanel = new FlowPanel();
		submitPanel.setStyleName("submitPanel");
		rootPanel.add(submitPanel);
		submitPanel.setWidth("800");
		submitPanel.setVisible(false);
		
		textBox = new TextBox();
		textBox.setVisibleLength(109);
		textBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					onSubmit();
				}
			}
		});
		textBox.setSize("", "30");
		submitPanel.add(textBox);
		
		Button btnSubmit = new Button("Submit");
		btnSubmit.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				onSubmit();
			}
		});
		btnSubmit.setHeight("30");
		submitPanel.add(btnSubmit);
		
		socket = GWTSocketIOConnectionFactory.INSTANCE.create(this, null, (short)0);
		socket.connect();
	}
	
	private void addLine(String line) {
		Element p = DOM.createElement("p");
		p.setInnerHTML(line);
		htmlPanel.getElement().appendChild(p);
		htmlPanel.getElement().setScrollTop(1000000);
	}
	
	private void onSubmit() {
		String text = textBox.getText();
		
		if (text.equals("/lclose")) {
			addLine("<em>closing...</em>");
			socket.close();
		} else if (text.equals("/ldisconnect")) {
			addLine("<em>disconnecting...</em>");
			socket.disconnect();
		} else {
			addLine("<b>you:</b> " + text);
			textBox.setText("");
			try {
				socket.sendMessage(text);
			} catch (SocketIOException e) {
				// Ignore. This wwon't happen in the GWT version.
			}
		}
	}

	@Override
	public void onConnect() {
		htmlPanel.setHTML("");
		submitPanel.setVisible(true);
	}

	@Override
	public void onDisconnect(DisconnectReason reason, String errorMessage) {
		submitPanel.setVisible(false);
		if (errorMessage != null) {
			addLine("<b>Disconnected["+reason+"]:</b> " + errorMessage);
		} else {
			addLine("<b>Disconnected["+reason+"]</b>");
		}
	}

	private void onMessage(JSONObject obj) {
		if (obj.containsKey("welcome")) {
			JSONString str = obj.get("welcome").isString();
			if (str != null) {
				addLine("<em><b>" + str.stringValue() + "</b></em>");
			}
		} else if (obj.containsKey("announcement")) {
			JSONString str = obj.get("announcement").isString();
			if (str != null) {
				addLine("<em>" + str.stringValue() + "</em>");
			}
		} else if (obj.containsKey("message")) {
			JSONArray arr = obj.get("message").isArray();
			if (arr != null && arr.size() >= 2) {
				JSONString id = arr.get(0).isString();
				JSONString msg = arr.get(1).isString();
				if (id != null && msg != null) {
					addLine("<b>" + id.stringValue() + ":</b> " + msg.stringValue());
				}
			}
		}
	}
	
	@Override
	public void onMessage(int messageType, String message) {
		if (messageType == 1) {
			JSONObject obj = JSONParser.parse(message).isObject();
			if (obj != null) {
				onMessage(obj);
			}
		}
	}
}
