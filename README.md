# SlipStream Web UI

An application that provides a graphical interface to cloud management
services that use the CIMI interface.  The tortured acronym comes from
"Cimi resoUrces via a Browser InterfaCe".

It is an alpha-level **prototype**, but in regular use by the
developers.  Feedback (as GitHub issues) on the UI, source code, and
underlying technologies is welcome.

## Frameworks and Libraries

SlipStream Code:

 * [SlipStream Clojure(Script)
   API](https://github.com/slipstream/SlipStreamClojureAPI): Provides
   a ClojureScript API for the CIMI and CIMI-like resources within
   SlipStream.

Frameworks:

 * [Reagent](https://github.com/reagent-project/reagent): Provides a
   ClojureScript interface to the
   [React](https://facebook.github.io/react/) framework.
 * [re-frame](https://github.com/Day8/re-frame): A framework that
   relies on React for visualization and provides mechanisms for using
   "FRP" for data flow and control.

Widgets:

 * [Semantic UI React](https://react.semantic-ui.com/introduction):
   React integration for Semantic UI.


## Development Environment

The essentials for using the development environment are below.

### Browser

To test the code on a SlipStream server (e.g. https://nuv.la/) running
on a different machine, you'll need to start a browser with the XSS
protections disabled.

For **Chrome** on MacOS, this can be done with:

```
$ open /Applications/Google\ Chrome.app \
       --args --disable-web-security --user-data-dir
```

For **Safari**, first enable the "Develop" menu.  Open the Safari
preferences, click the "Advanced" tab, and then activate the "Show
Develop menu in menu bar" option.  Once the "Develop" menu is visible,
choose the "Disable Cross-Origin Restrictions" option.

There may be **FireFox** plugins that will allow you to disable the
CORS protections.  Easier solution is to use Chrome or Safari.

### Development
The development environment requires [`lein`](https://leiningen.org).

Once `lein` is installed, you can setup the interactive environment by
doing the following:

 * In a terminal, start development server for the webui.

     ```
     $ lein dev
     ```

 * You will get automatically a REPL, with Figwheel controls:

     ```
     dev:cljs.user=>
     ```

 * Wait a bit, then browse to
 [http://localhost:3000/webui.html](http://localhost:3000/webui.html).


You should see the client application running.  Any changes you make
to the source files (either ClojureScript sources or HTML templates)
should be reflected immediately in the browser.

### Testing

* In a terminal, start tests for the webui.

     ```
     $ lein test
     ```

* You can also get automatic tests re-execution triggered on your code
  change with following command :

     ```
     $ lein test-auto
     ```

## Integration with IntelliJ

You can import the repository through IntelliJ, using the "leiningen"
method in the dialog.

### Logging

You can reset the logging level for kvlt from the REPL when running
in development mode. From the REPL do:

```
=> (require '[taoensso.timbre :as timbre])
=> (timbre/set-level! :info)
```

The default value is `:debug` which will log all of the HTTP requests
and responses.  This is useful when debugging interactions with
SlipStream, but annoying otherwise.
