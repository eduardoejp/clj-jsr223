;; Copyright (C) 2012, Eduardo Julián. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the 
;; Eclipse Public License 1.0
;; (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;; in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns ^{:author "Eduardo Julián <eduardoejp@gmail.com>",
      :doc "Clojure implementations of the ScriptEngine and ScriptEngineFactory interfaces."}
     clj-jsr223
  (:import (javax.script Bindings ScriptContext)))

(deftype ClojureScriptEngine [factory context]
  javax.script.ScriptEngine
  (getFactory [_] factory)
  (getContext [_] @context)
  (setContext [_ ctx] (reset! context ctx))
  (createBindings [_] (javax.script.SimpleBindings.))
  (getBindings [_ ctx] (.getBindings @context ctx))
  (setBindings [_ binds ctx] (.setBindings @context binds ctx))
  (put [self k v] (.put (.getBindings self) k v))
  (eval ^Object [self ^String script] (eval (read-string script)))
  (eval ^Object [self ^String script ^Bindings binds]
    (doseq [[k v] (->> binds seq (apply conj {}))]
      (let [parts (.split k "/")
            [ns name] (if (= 2 (count parts)) (map symbol parts) ['user (symbol (first parts))])]
        (create-ns ns)
        (intern ns name v)))
    (.eval self script))
  (eval ^Object [self ^String script ^ScriptContext ctx] (.eval self script (.getBindings ctx)))
  (eval ^Object [self ^java.io.Reader script] (.eval self (slurp script)))
  (eval ^Object [self ^java.io.Reader script ^Bindings binds] (.eval self (slurp script) binds))
  (eval ^Object [self ^java.io.Reader script ^ScriptContext ctx] (.eval self (slurp script) (.getBindings ctx)))
  )

(deftype ClojureScriptEngineFactory []
  javax.script.ScriptEngineFactory
  (getParameter [_ _] nil)
  (getEngineName [_] "Clojure Scripting Engine")
  (getEngineVersion [_] "1.4")
  (getLanguageName [_] "Clojure")
  (getLanguageVersion [_] "1.4")
  (getExtensions [_] ["clj"])
  (getMimeTypes [_] ["application/clojure" "text/clojure"])
  (getNames [_] ["clojure" "Clojure"])
  (getScriptEngine [self] (ClojureScriptEngine. self (atom (javax.script.SimpleScriptContext.))))
  (getOutputStatement [_ to-print] (str "(println \"" to-print "\")"))
  (getMethodCallSyntax [_ obj meth args] (str "(." meth " " obj " " (apply str (interpose " " (map pr-str args))) ")"))
  (getProgram [_ forms] (apply str (interleave forms (repeat \newline)))))
