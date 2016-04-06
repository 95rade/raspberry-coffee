package jsr223;

import javax.script.*;
import java.io.*;
import java.util.List;

/**
 * Scripting from Java 101.
 * Groovy and JavaScript
 */
public class ScriptEngineFactories
{
  private static ScriptEngineFactories instance = null;

  public static void main(String[] args)
  {
    instance = new ScriptEngineFactories();

    List<ScriptEngineFactory> factories = new ScriptEngineManager().getEngineFactories();
    System.out.println("=======================");
    for (ScriptEngineFactory factory : factories)
    {
      System.out.println("Lang name:" + factory.getLanguageName());
      System.out.println("Engine name:" + factory.getEngineName());
      System.out.println(factory.getNames().toString());
    }
    System.out.println("=======================");
    System.out.println("Act 1: Groovy.");
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
//  Writer writer = new StringWriter();
//  engine.getContext().setWriter(writer);
    try
    {
      engine.eval("println 'Hello Groovy!'");
      System.out.println(">>> Executing src/mainBasic.groovy");
      engine.eval(new FileReader("src/mainBasic.groovy"));
 //   String output = writer.toString();
 //   System.out.println("Output:[" + output + "]");
    }
    catch (ScriptException se)
    {
      se.printStackTrace();
    }
    catch (FileNotFoundException fnfe)
    {
      System.err.println("From " + new File(".").getAbsolutePath() + ":");
      fnfe.printStackTrace();
    }
    // Execute Groovy function on a groovy object
    try
    {
      // Act as mainBasic.groovy
      engine.eval(new FileReader("src/GroovyBasic.groovy"));
      engine.eval("app = new GroovyBasic()");
      Object app = engine.get("app");
      Invocable invocable = (Invocable)engine;
      System.out.println(">> Invoking hello() method on GroovyBasic object...");
      invocable.invokeMethod(app, "hello"); // No prm
      System.out.println("=== Done ===");
    }
    catch (ScriptException se)
    {
      se.printStackTrace();
    }
    catch (FileNotFoundException fnfe)
    {
      System.err.println("From " + new File(".").getAbsolutePath() + ":");
      fnfe.printStackTrace();
    }
    catch (NoSuchMethodException nsme)
    {
      nsme.printStackTrace();
    }

    System.out.println("Act 2: JavaScript.");
    engine = new ScriptEngineManager().getEngineByName("javascript");
    try
    {
      engine.eval("print('Hello Nashorn!');");
    }
    catch (ScriptException se)
    {
      se.printStackTrace();
    }
    System.out.println("Bye.");
  }
}
