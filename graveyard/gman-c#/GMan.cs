/*
 * This file is part of GMan.
 * Copyright (C) 2007 Elliott Hughes <enh@jessies.org>.
 * 
 * GMan is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GMan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

using System;
using System.Collections;
using System.Diagnostics;
using System.Net;
using System.IO;
using System.Text.RegularExpressions;
using Gtk;
using GtkSharp;

public class GMan {
    private Gtk.Window window;
    private Gtk.Entry entry;
    private Gtk.HTML html;
    private Gtk.Statusbar statusBar;
    private string currentUrl;
    
    private static string STL_DOCUMENTATION_ROOT = "/usr/share/doc/stl-manual/html/";
    private static Hashtable stlTermFilenames;
    
    public static void Main(string[] args) {
        new GMan();
    }
    
    public GMan() {
        Application.Init();
        
        window = new Gtk.Window("GMan");
        window.SetDefaultSize(800, 600);
        window.DeleteEvent += new DeleteEventHandler(OnWindowDelete);
        // FIXME: we should remember where we were when we last ran.
        window.WindowPosition = WindowPosition.Center;
        
        VBox vbox = new VBox(false, 2);
        window.Add(vbox);
        
        vbox.PackStart(MakeMenuBar(), false, false, 1);
        
        HBox hbox = new HBox(false, 1);
        
        Label label = new Label("Search:");
        
        entry = new Gtk.Entry("");
        entry.Activated += new EventHandler(OnEntryActivated);
        
        Button button = new Button("!");
        button.Clicked += new EventHandler(OnButtonClicked);
        
        Button backButton = new Button(Stock.GoBack);
        Button forwardButton = new Button(Stock.GoForward);
        
        hbox.PackStart(backButton, false, false, 1);
        hbox.PackStart(forwardButton, false, false, 1);
        hbox.PackStart(label, false, false, 1);
        hbox.PackStart(entry, true, true, 1);
        hbox.PackStart(button, false, false, 1);
                        
        vbox.PackStart(hbox, false, false, 1);
        
        ScrolledWindow sw = new ScrolledWindow();
        sw.VscrollbarPolicy = PolicyType.Always;
        sw.HscrollbarPolicy = PolicyType.Always;
        vbox.PackStart(sw, true, true, 1);
        
        statusBar = new Gtk.Statusbar();
        vbox.PackStart(statusBar, false, false, 1);
        
        html = new HTML();
        html.LinkClicked += new LinkClickedHandler(OnLinkClicked);
        html.OnUrl += new OnUrlHandler(OnOnUrl);
        html.TitleChanged += new TitleChangedHandler(OnTitleChanged);
        sw.Add(html);
        
        window.ShowAll();
        entry.GrabFocus();
        Application.Run();
    }
    
    private MenuBar MakeMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.Append(MakeFileMenu());
        menuBar.Append(MakeEditMenu());
        menuBar.Append(MakeHelpMenu());
        return menuBar;
    }
    
    private MenuItem MakeFileMenu() {
        Menu menu = new Menu();
        AccelGroup group = new AccelGroup();
        window.AddAccelGroup(group);
        
        ImageMenuItem quit = new ImageMenuItem(Stock.Quit, group);
        quit.AddAccelerator("activate", group, new AccelKey(Gdk.Key.q, Gdk.ModifierType.ControlMask, AccelFlags.Visible));
        quit.Activated += new EventHandler(OnFileQuit);
        menu.Append(quit);
        
        MenuItem label = new MenuItem("_File");
        label.Submenu = menu;
        return label;
    }
    
    private MenuItem MakeEditMenu() {
        Menu menu = new Menu();
        AccelGroup group = new AccelGroup();
        window.AddAccelGroup(group);
        
        ImageMenuItem copy = new ImageMenuItem(Stock.Copy, group);
        copy.AddAccelerator("activate", group, new AccelKey(Gdk.Key.c, Gdk.ModifierType.ControlMask, AccelFlags.Visible));
        //copy.Activated += new EventHandler(OnEditCopy);
        menu.Append(copy);
        
        ImageMenuItem selectAll = new ImageMenuItem(Stock.SelectAll, group);
        selectAll.AddAccelerator("activate", group, new AccelKey(Gdk.Key.a, Gdk.ModifierType.ControlMask, AccelFlags.Visible));
        //selectAll.Activated += new EventHandler(OnEditSelectAll);
        menu.Append(selectAll);
        
        MenuItem label = new MenuItem("_Edit");
        label.Submenu = menu;
        return label;
    }
    
    private MenuItem MakeHelpMenu() {
        Menu menu = new Menu();
        AccelGroup group = new AccelGroup();
        window.AddAccelGroup(group);
        
        ImageMenuItem about = new ImageMenuItem(Stock.About, group);
        about.Activated += new EventHandler(OnHelpAbout);
        menu.Append(about);
        
        MenuItem label = new MenuItem("_Help");
        label.Submenu = menu;
        return label;
    }
    
    private void OnFileQuit(object o, EventArgs args) {
        Application.Quit();
    }
    
    private void OnHelpAbout(object o, EventArgs args) {
        Gtk.AboutDialog aboutDialog;
        aboutDialog = new Gtk.AboutDialog();
        aboutDialog.Name = "GMan";
        aboutDialog.Version = "0.1";
        aboutDialog.Copyright = "\u00a9 2007 Elliott Hughes";
        aboutDialog.Comments = "Documentation viewer for programmers.";
        aboutDialog.Authors = new string[] { "Elliott Hughes <enh@jessies.org>" };
        aboutDialog.Logo = new Gdk.Pixbuf("/home/elliotth/Projects/evergreen/lib/evergreen-32.png");
        aboutDialog.TransientFor = window;
        aboutDialog.Run();
        aboutDialog.Destroy();
    }
    
    private void OnTitleChanged(object obj, TitleChangedArgs args) {
        window.Title = args.NewTitle + " - GMan";
    }
    
    private void OnWindowDelete(object obj, DeleteEventArgs args) {
        Application.Quit();
    }
    
    private void OnButtonClicked(object obj, EventArgs args) {
    }
    
    private string findPolyglotMan() {
        return "/usr/bin/rman"; // FIXME
    }
    
    private static string backQuote(string command) {
        ProcessStartInfo processStartInfo = new ProcessStartInfo("/bin/bash", "-c '" + command + "'"); // FIXME: robustness!
        processStartInfo.RedirectStandardOutput = true;
        processStartInfo.UseShellExecute = false;
        Process p = Process.Start(processStartInfo);
        string output = p.StandardOutput.ReadToEnd();
        // FIXME: what about stderr?
        p.WaitForExit();
        return output;
    }
    
    private void OnEntryActivated(object obj, EventArgs args) {
        string text = entry.Text.Trim();
        if (text.StartsWith("man:")) {
            HandleManUrl(text);
        } else if (text.StartsWith("stl:")) {
            HandleStlUrl(text);
        } else {
            // have a guess...
            // FIXME: we could guess better by having catalogs of man pages and the like, and seeing what we've got.
            ShowManPage("2:3", text);
        }
    }
    
    private void ShowManPage(string section, string page) {
        string command = "man -S " + section + " " + page + " | col -b";
        string polyglotMan = findPolyglotMan();
        if (polyglotMan != null) {
            polyglotMan = polyglotMan + " -f HTML -r man:%s\\(%s\\)";
            string filename = backQuote("man -S " + section + " -w " + page).Split(new char[] { '\n' })[0];
            command = (filename.EndsWith(".gz") ? "gunzip -c" : "cat") + " '" + filename + "' | " + polyglotMan + " -S ";
        }
        
        Console.WriteLine(command);
        string result = backQuote(command);
        if (polyglotMan != null) {
            // Tidy up the PolyglotMan (rman) output.
            
            // There are case differences between 3.0.9 on Mac OS and 3.2 on Linux.
            // There's also a difference in the quote character used: ' on Linux and " on Mac OS.
            
            // Remove the table of contents at the end. There may be nested lists because subsection are included.
            result = Regex.Replace(result, "(?si)<hr><p>.*</ul>", "");
            // Remove the links to the table of contents.
            result = Regex.Replace(result, "(?i)<a name=['\"]sect\\d+['\"] href=['\"]#toc\\d+['\"]>((.|\n)*?)</a>", "$1");
            result = Regex.Replace(result, "(?i)<a href=['\"]#toc['\"]>Table of Contents</a><p>", "");
        }
        
        ShowHtml(result);
    }
    
    private void ShowHtml(string htmlText) {
        Console.WriteLine(htmlText);
        HTMLStream htmlStream = html.Begin();
        htmlStream.Write(htmlText);
        html.End(htmlStream, HTMLStreamStatus.Ok);
    }
    
    private void HandleManUrl(string url) {
        // FIXME: error handling!
        Match match = Regex.Match(url, "^man:(.+)\\((.+)\\)$");
        if (match.Success) {
            string page = match.Groups[1].Value;
            string section = match.Groups[2].Value;
            ShowManPage(section, page);
        }
    }
    
    private void HandleStlUrl(string url) {
        // FIXME: error handling!
        Match match = Regex.Match(url, "^stl:([A-Za-z0-9_]+)$");
        if (match.Success) {
            string term = match.Groups[1].Value;
            if (term.StartsWith("std::")) {
                term = term.Substring(5);
            }
            ShowHtml(GetStlDocumentation(term));
        }
    }
    
    private string GetStlDocumentation(string term) {
        InitStlTermFilenames();
        // Return the HTML if we have it. The STL documentation is simple enough for us to render it ourselves.
        string filename = (string) stlTermFilenames[term];
        string result = null;
        if (filename != null) {
            result = File.ReadAllText(filename);
            // Fix relative links.
            result = Regex.Replace(result, "(?i)<head>", "<head><base href=\"file://" + STL_DOCUMENTATION_ROOT + "\">");
            // Tidy up the member tables by making all TT-only TD elements use PRE instead.
            result = Regex.Replace(result, "(?i)\n<tt>(.*?)</tt>\n</td>", "\n<pre>$1</pre></td>\n");
        }
        return result;
    }
    
    private void InitStlTermFilenames() {
        if (stlTermFilenames != null) {
            return;
        }
        stlTermFilenames = new Hashtable();
        if (Directory.Exists(STL_DOCUMENTATION_ROOT) == false) {
            Console.WriteLine("STL documentation not installed. install package \"stl-manual\".");
            return;
        }
        Regex pattern = new Regex(".*<A href=\"(.+\\.html)\">([a-z0-9_]+)(&lt;.*&gt;)?</A></TD>.*");
        string[] lines = File.ReadAllLines(STL_DOCUMENTATION_ROOT + "stl_index.html");
        foreach (string line in lines) {
            Match match = pattern.Match(line);
            if (match.Success) {
                string filename = STL_DOCUMENTATION_ROOT + match.Groups[1].Value;
                string term = match.Groups[2].Value;
                stlTermFilenames[term] = filename;
                
                // std::string and std::wstring are typedefs for std::basic_string.
                if (term == "basic_string") {
                    stlTermFilenames["string"] = stlTermFilenames["wstring"] = filename;
                }
            }
        }
        Console.WriteLine("Learned of " + stlTermFilenames.Count + " STL terms.");
    }
    
    private void OnLinkClicked(object obj, LinkClickedArgs args) {
        if (args.Url.StartsWith("man:")) {
            HandleManUrl(args.Url);
        } else if (args.Url.StartsWith("file://")) {
            // FIXME: install a handler for file: urls with System.Net.WebRequest.RegisterPrefix.
            ShowHtml(File.ReadAllText(args.Url.Substring(7)));
        } else if (args.Url.StartsWith("http://")) {
            ShowUrl(args.Url);
        } else {
            // Build up the relative URL.
            ShowUrl(currentUrl + args.Url);
        }
    }
    
    private void ShowUrl(string newUrl) {
        Console.WriteLine(newUrl);
        try {
            LoadHtml(newUrl);
        } catch {
        }
        currentUrl = newUrl;
    }
    
    private uint lastUrlId = 0;
    
    private void OnOnUrl(object o, OnUrlArgs args) {
        if (lastUrlId != 0) {
            statusBar.Remove(statusBar.GetContextId("OnOnUrl"), lastUrlId);
        }
        if (args.Url != null) {
            lastUrlId = statusBar.Push(statusBar.GetContextId("OnOnUrl"), args.Url);
        }
    }
    
    private void LoadHtml(string URL) {
        HttpWebRequest web_request = (HttpWebRequest) WebRequest.Create(URL);
        HttpWebResponse web_response = (HttpWebResponse) web_request.GetResponse();
        Stream stream = web_response.GetResponseStream();
        byte [] buffer = new byte [8192];
        
        HTMLStream html_stream = html.Begin();
        int count;
        while ((count = stream.Read(buffer, 0, 8192)) != 0) {
            html_stream.Write(buffer, count);
        }
        html.End(html_stream, HTMLStreamStatus.Ok);
    }
}
