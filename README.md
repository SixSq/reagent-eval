# Reagent Evaluation

## Purpose

This repository contains a simple browser-side client for SlipStream.
The purpose of this is to understand how to use the SlipStream
Clojure(Script) API from the browser and to see how Reagent works as a
framework for the browser-side UI.

## Useful Links

Code:

 * [SlipStream Clojure(Script)
   API](https://github.com/slipstream/SlipStreamClientAPI )
 * [Reagent](https://github.com/reagent-project/reagent)
 * [React-Bootstrap](https://react-bootstrap.github.io)

Helpful Information:

 * [Modern
   ClojureScript](https://github.com/magomimmo/modern-cljs/tree/master/doc/second-edition)
 * [Reagent Introduction](https://reagent-project.github.io)
 * [Bootstrap with
   Reagent](http://nicolovaligi.com/boostrap-components-reagent-clojurescript.html) 

## Development Environment

### Browser

To test the code on a SlipStream server (e.g. https://nuv.la/) running
on a different machine, you'll need to start a browser with the XSS
protections disabled.  For Chrome on OS X, this can be done with:

```
$ open /Applications/Google\ Chrome.app \
       --args --disable-web-security --user-data-dir
```

### Boot

The development environment requires [`boot`](http://boot-clj.com).

Once `boot` is installed, you can setup the interactive environment by
doing the following:

 * In a terminal, start development server `boot dev`.
     ```
     $ boot dev
     ```
 * In another terminal, start the REPL:
     ```
     $ boot repl -c
     boot.user=> (start-repl)
     ...
     cljs.user=> 
     ```
 * Point your browser to http://localhost:3000/.

You should see the client application running.  Any changes you make
to the source files (either ClojureScript sources or HTML templates)
should be reflected immediately in the browser.

## Integration with IntelliJ

TBD...

## Current Status

The test application provides:

 * A simple form for logging into the SlipStream server,
 * A section to visualize the cloud entry point (along with buttons to
   clear and retrieve the value), and
 * A section that gives the number of "event" resources visible to the
   user. 

All of these sections work, although the cookie (token) handling will
need to be redesigned to make it more compatible with browsers.

**Running the application currently requires that you build a local
  copy of the SlipStream Clojure client from the branch
  `feature/using-cljs`.**
