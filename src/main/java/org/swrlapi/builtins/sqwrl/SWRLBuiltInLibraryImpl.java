package org.swrlapi.builtins.sqwrl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.swrlapi.builtins.AbstractSWRLBuiltInLibrary;
import org.swrlapi.builtins.arguments.*;
import org.swrlapi.exceptions.InvalidSWRLBuiltInArgumentException;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.sqwrl.SQWRLResultGenerator;
import org.swrlapi.sqwrl.values.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Implementation library for SQWRL built-ins.
 * <p>
 * Unlike other built-in libraries, queries that use built-ins in this library need to be preprocessed by a SQWRL-aware
 * processor. See the <tt>org.swrlapi.factory.DefaultSQWRLQuery</tt> class for an example of this processing.
 */
public class SWRLBuiltInLibraryImpl extends AbstractSWRLBuiltInLibrary
{
  private static final String PREFIX = "sqwrl";

  private static final String NAMESPACE = "http://sqwrl.stanford.edu/ontologies/built-ins/3.4/sqwrl.owl#";

  private static final String[] BUILT_IN_NAMES = { "selectDistinct", "select", "count", "columnNames", "orderBy",
    "orderByDescending", "limit", "min", "max", "avg", "sum", "median", "makeSet", "makeBag", "groupBy", "size",
    "isEmpty", "notEmpty", "element", "notElement", "intersects", "notIntersects", "equal", "notEqual", "contains",
    "notContains", "difference", "union", "intersection", "append", "last", "notLast", "lastN", "notLastN", "first",
    "notFirst", "firstN", "notFirstN", "nth", "notNth", "nthLast", "notNthLast", "nthSlice", "notNthSlice",
    "nthLastSlice", "notNthLastSlice", "greatest", "notGreatest", "greatestN", "notGreatestN", "least", "notLeast",
    "leastN", "notLeastN", "nthGreatest", "notNthGreatest", "nthGreatestSlice", "notNthGreatestSlice" };

  /**
   * A collections map is a map of collection keys to a map of group keys to collections.
   * A collection can be uniquely identified by its query name and collection name. A unique key is generated from this
   * combination. If a collection is grouped, each group will be have a unique key generated.
   */
  @NonNull private final Map<@NonNull String, @NonNull Map<@NonNull String, @NonNull Collection<@NonNull SWRLBuiltInArgument>>> collectionsMap;

  /**
   * A map of collection keys to group size (which will be 0 for ungrouped collections)
   */
  @NonNull private final Map<@NonNull String, @NonNull Integer> collectionGroupElementNumbersMap;

  @NonNull private final Set<@NonNull String> setKeys, bagKeys;

  public SWRLBuiltInLibraryImpl()
  {
    super(PREFIX, NAMESPACE, new HashSet<>(Arrays.asList(BUILT_IN_NAMES)));

    this.collectionsMap = new HashMap<>();
    this.collectionGroupElementNumbersMap = new HashMap<>();
    this.setKeys = new HashSet<>();
    this.bagKeys = new HashSet<>();
  }

  @Override public void reset()
  {
    this.collectionsMap.clear();
    this.collectionGroupElementNumbersMap.clear();
    this.setKeys.clear();
    this.bagKeys.clear();
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean select(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    checkForUnboundArguments(arguments);
    checkNumberOfArgumentsAtLeastOne(arguments);
    SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());

    if (!resultGenerator.isRowOpen())
      resultGenerator.openRow();

    int argumentIndex = 0;
    for (SWRLBuiltInArgument argument : arguments) {
      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        resultGenerator.addCell(literal);
      } else if (argument instanceof SWRLNamedIndividualBuiltInArgument) {
        SWRLNamedIndividualBuiltInArgument individualArgument = (SWRLNamedIndividualBuiltInArgument)argument;
        SQWRLNamedIndividualResultValue individualValue = getSQWRLResultValueFactory()
          .getNamedIndividualValue(individualArgument);
        resultGenerator.addCell(individualValue);
      } else if (argument instanceof SWRLClassBuiltInArgument) {
        SWRLClassBuiltInArgument classArgument = (SWRLClassBuiltInArgument)argument;
        SQWRLClassResultValue classValue = getSQWRLResultValueFactory().getClassValue(classArgument);
        resultGenerator.addCell(classValue);
      } else if (argument instanceof SWRLObjectPropertyBuiltInArgument) {
        SWRLObjectPropertyBuiltInArgument objectPropertyArgument = (SWRLObjectPropertyBuiltInArgument)argument;
        SQWRLObjectPropertyResultValue objectPropertyValue = getSQWRLResultValueFactory()
          .getObjectPropertyValue(objectPropertyArgument);
        resultGenerator.addCell(objectPropertyValue);
      } else if (argument instanceof SWRLDataPropertyBuiltInArgument) {
        SWRLDataPropertyBuiltInArgument dataPropertyArgument = (SWRLDataPropertyBuiltInArgument)argument;
        SQWRLDataPropertyResultValue dataPropertyValue = getSQWRLResultValueFactory()
          .getDataPropertyValue(dataPropertyArgument);
        resultGenerator.addCell(dataPropertyValue);
      } else if (argument instanceof SWRLAnnotationPropertyBuiltInArgument) {
        SWRLAnnotationPropertyBuiltInArgument annotationPropertyArgument = (SWRLAnnotationPropertyBuiltInArgument)argument;
        SQWRLAnnotationPropertyResultValue annotationPropertyValue = getSQWRLResultValueFactory()
          .getAnnotationPropertyValue(annotationPropertyArgument);
        resultGenerator.addCell(annotationPropertyValue);
      } else if (argument instanceof SWRLClassExpressionBuiltInArgument) {
        SWRLClassExpressionBuiltInArgument classExpressionArgument = (SWRLClassExpressionBuiltInArgument)argument;
        SQWRLClassExpressionResultValue classExpressionValue = getSQWRLResultValueFactory()
          .getClassExpressionValue(classExpressionArgument);
        resultGenerator.addCell(classExpressionValue);
      } else if (argument instanceof SQWRLCollectionVariableBuiltInArgument) {
        throw new InvalidSWRLBuiltInArgumentException(argumentIndex, "collections cannot be selected");
      } else
        throw new InvalidSWRLBuiltInArgumentException(argumentIndex,
          "unknown type " + argument.getClass().getCanonicalName());
      argumentIndex++;
    }
    return false;
  }

  /**
   * Preprocessed to signal that duplicates should be removed from result
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean selectDistinct(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();

    return select(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean count(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    checkForUnboundArguments(arguments);
    checkNumberOfArgumentsEqualTo(1, arguments.size());

    SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
    SWRLBuiltInArgument argument = arguments.get(0);

    if (!resultGenerator.isRowOpen())
      resultGenerator.openRow();

    if (argument instanceof SWRLLiteralBuiltInArgument) {
      SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
      SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
      resultGenerator.addCell(literal);
    } else if (argument instanceof SWRLNamedIndividualBuiltInArgument) {
      SWRLNamedIndividualBuiltInArgument individualArgument = (SWRLNamedIndividualBuiltInArgument)argument;
      SQWRLNamedIndividualResultValue individualValue = getSQWRLResultValueFactory()
        .getNamedIndividualValue(individualArgument);
      resultGenerator.addCell(individualValue);
    } else if (argument instanceof SWRLClassBuiltInArgument) {
      SWRLClassBuiltInArgument classArgument = (SWRLClassBuiltInArgument)argument;
      SQWRLClassResultValue classValue = getSQWRLResultValueFactory().getClassValue(classArgument);
      resultGenerator.addCell(classValue);
    } else if (argument instanceof SWRLObjectPropertyBuiltInArgument) {
      SWRLObjectPropertyBuiltInArgument objectPropertyArgument = (SWRLObjectPropertyBuiltInArgument)argument;
      SQWRLObjectPropertyResultValue objectPropertyValue = getSQWRLResultValueFactory()
        .getObjectPropertyValue(objectPropertyArgument);
      resultGenerator.addCell(objectPropertyValue);
    } else if (argument instanceof SWRLDataPropertyBuiltInArgument) {
      SWRLDataPropertyBuiltInArgument dataPropertyArgument = (SWRLDataPropertyBuiltInArgument)argument;
      SQWRLDataPropertyResultValue dataPropertyValue = getSQWRLResultValueFactory()
        .getDataPropertyValue(dataPropertyArgument);
      resultGenerator.addCell(dataPropertyValue);
    } else if (argument instanceof SWRLAnnotationPropertyBuiltInArgument) {
      SWRLAnnotationPropertyBuiltInArgument annotationPropertyArgument = (SWRLAnnotationPropertyBuiltInArgument)argument;
      SQWRLAnnotationPropertyResultValue annotationPropertyValue = getSQWRLResultValueFactory()
        .getAnnotationPropertyValue(annotationPropertyArgument);
      resultGenerator.addCell(annotationPropertyValue);
    } else if (argument instanceof SQWRLCollectionVariableBuiltInArgument) {
      throw new InvalidSWRLBuiltInArgumentException(0, "collections cannot be counted");
    } else
      throw new InvalidSWRLBuiltInArgumentException(0, "unknown type " + argument.getClass().getCanonicalName());

    return false;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean countDistinct(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  { // This built-in is preprocessed by SWRLProcessor so there is nothing to do here
    checkThatInConsequent();
    return count(arguments);
  }

  /**
   * This built-in is preprocessed by SWRLProcessor so there is nothing to do here.a
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean columnNames(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    return true;
  }

  /**
   * This built-in is preprocessed by SWRLAPIProcessor so there is nothing to do here.a
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean orderBy(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    return true;
  }

  /**
   * This built-in is preprocessed by SWRLProcessor so there is nothing to do here.a
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean orderByDescending(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    return true;
  }

  /**
   * This built-in is preprocessed by SWRLProcessor so there is nothing to do here.a
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean limit(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInConsequent();
    return true;
  }

  /*
   * ******************************************************************************************************************
   * SQWRL collection operators
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean makeSet(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();
    checkForUnboundNonFirstArguments(arguments);

    final int resultCollectionArgumentNumber = 0, elementArgumentNumber = 1;
    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collectionGroupKey = getCollectionGroupKeyInMake(arguments); // Get unique key for collection group (if any);
    // does argument checking
    SWRLBuiltInArgument element = arguments.get(elementArgumentNumber);
    Collection<SWRLBuiltInArgument> set;

    if (isCollection(queryName, collectionName, collectionGroupKey))
      set = getCollection(queryName, collectionName, collectionGroupKey);
    else
      set = createSet(queryName, collectionName, collectionGroupKey);

    set.add(element);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();
      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, collectionName, collectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean makeBag(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();
    checkForUnboundNonFirstArguments(arguments);

    final int resultCollectionArgumentNumber = 0, elementArgumentNumber = 1;
    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collectionGroupKey = getCollectionGroupKeyInMake(arguments); // Get unique key for bag; does argument
    // checking
    SWRLBuiltInArgument element = arguments.get(elementArgumentNumber);
    Collection<SWRLBuiltInArgument> bag;

    if (isCollection(queryName, collectionName, collectionGroupKey))
      bag = getCollection(queryName, collectionName, collectionGroupKey);
    else
      bag = createBag(queryName, collectionName, collectionGroupKey);

    bag.add(element);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, collectionName, collectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /**
   * This built-in is preprocessed by SWRLProcessor so there is nothing to do here.a
   *
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean groupBy(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return true;
  }

  /*
   * ******************************************************************************************************************
   * SQWRL operators that work with a single collection and return a value or an element or evaluate to true or false
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean isEmpty(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int sourceCollectionArgumentNumber = 0, numberOfCoreArguments = 1;
    Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
      sourceCollectionArgumentNumber, numberOfCoreArguments);

    return collection.size() == 0;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notEmpty(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return !isEmpty(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean size(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;
    Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
      sourceCollectionArgumentNumber, numberOfCoreArguments);

    return processResultArgument(arguments, resultArgumentNumber, BigInteger.valueOf(collection.size()));
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean element(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;

    Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
      sourceCollectionArgumentNumber, numberOfCoreArguments);

    return processResultArgument(arguments, resultArgumentNumber, collection);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notElement(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return !element(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean min(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, numberOfConsequentArguments = 1;

    if (getIsInConsequent()) { // Simple SQWRL aggregation; this is post processed by a SQWRLResult implementation
      checkForUnboundArguments(arguments);
      checkNumberOfArgumentsEqualTo(numberOfConsequentArguments, arguments.size());

      SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
      SWRLBuiltInArgument argument = arguments.get(resultArgumentNumber);

      if (!resultGenerator.isRowOpen())
        resultGenerator.openRow();

      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        if (literal.isNumeric()) {
          resultGenerator.addCell(literal);
        } else
          throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
            "expecting numeric literal, got " + argument);
      } else
        throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
          "expecting numeric literal, got " + argument);

      return true;
    } else
      return least(arguments); // Redirect to SQWRL collection operator
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean max(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, numberOfConsequentArguments = 1;

    if (getIsInConsequent()) { // SQWRL aggregation operator; this is post processed by a SQWRLResult implementation
      checkForUnboundArguments(arguments);
      checkNumberOfArgumentsEqualTo(numberOfConsequentArguments, arguments.size());

      SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
      SWRLBuiltInArgument argument = arguments.get(resultArgumentNumber);

      if (!resultGenerator.isRowOpen())
        resultGenerator.openRow();

      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        if (literal.isNumeric()) {
          resultGenerator.addCell(literal);
        } else
          throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
            "expecting numeric literal, got: " + argument);
      } else
        throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
          "expecting numeric literal, got: " + argument);

      return true;
    } else
      return greatest(arguments); // Redirect to SQWRL collection operator
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean sum(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1;
    final int numberOfCoreAntecedentArguments = 2, numberOfConsequentArguments = 1;

    if (getIsInConsequent()) { // SQWRL aggregation operator; this is post processed by a SQWRLResult implementation
      checkForUnboundArguments(arguments);
      checkNumberOfArgumentsEqualTo(numberOfConsequentArguments, arguments.size());

      SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
      SWRLBuiltInArgument argument = arguments.get(resultArgumentNumber);

      if (!resultGenerator.isRowOpen())
        resultGenerator.openRow();

      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        if (literal.isNumeric())
          resultGenerator.addCell(literal);
        else
          throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
            "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());
      } else
        throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
          "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());

      return true;
    } else { // SQWRL collection operator
      Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreAntecedentArguments);

      if (collection.isEmpty())
        return false;
      else {
        BigDecimal sumValue = BigDecimal.ZERO;
        for (SWRLBuiltInArgument element : collection) {
          checkThatElementIsComparable(element);
          BigDecimal value = getArgumentAsADecimal(element);
          sumValue = sumValue.add(value);
        }
        SWRLBuiltInArgument resultArgument = createLeastNarrowNumericLiteralBuiltInArgument(sumValue,
          new ArrayList<>(collection));

        return processResultArgument(arguments, resultArgumentNumber, resultArgument);
      }
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean avg(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreAntecedentArguments = 2;
    final int numberOfConsequentArguments = 1;

    if (getIsInConsequent()) { // Simple SQWRL aggregation operator
      checkForUnboundArguments(arguments);
      checkNumberOfArgumentsEqualTo(numberOfConsequentArguments, arguments.size());

      SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
      SWRLArgument argument = arguments.get(0);

      if (!resultGenerator.isRowOpen())
        resultGenerator.openRow();

      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        if (literal.isNumeric())
          resultGenerator.addCell(literal);
        else
          throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
            "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());
        return false;
      } else
        throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
          "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());
    } else { // SQWRL collection operator
      Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreAntecedentArguments);

      if (collection.isEmpty())
        return false;
      else {
        BigDecimal sumValue = BigDecimal.ZERO;
        for (SWRLBuiltInArgument element : collection) {
          checkThatElementIsComparable(element);
          BigDecimal value = getArgumentAsADecimal(element);
          sumValue = sumValue.add(value);
        }
        BigDecimal avgValue = sumValue.divide(BigDecimal.valueOf(collection.size()));
        SWRLBuiltInArgument resultArgument = createLeastNarrowNumericLiteralBuiltInArgument(avgValue,
          new ArrayList<>(collection));

        return processResultArgument(arguments, resultArgumentNumber, resultArgument);
      }
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean median(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1;
    final int numberOfCoreAntecedentArguments = 2, numberOfConsequentArguments = 1;

    if (getIsInConsequent()) { // Simple SQWRL aggregation operator
      checkForUnboundArguments(arguments);
      checkNumberOfArgumentsEqualTo(numberOfConsequentArguments, arguments.size());

      SQWRLResultGenerator resultGenerator = getSQWRLResultGenerator(getInvokingRuleName());
      SWRLArgument argument = arguments.get(0);

      if (!resultGenerator.isRowOpen())
        resultGenerator.openRow();

      if (argument instanceof SWRLLiteralBuiltInArgument) {
        SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
        SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
        if (literal.isNumeric())
          resultGenerator.addCell(literal);
        else
          throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
            "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());
        return false;
      } else
        throw new InvalidSWRLBuiltInArgumentException(resultArgumentNumber,
          "expecting numeric literal, got " + argument + " with type " + argument.getClass().getCanonicalName());
    } else { // SQWRL collection operator
      Collection<SWRLBuiltInArgument> collection = getCollectionInSingleCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreAntecedentArguments);

      if (collection.isEmpty())
        return false;
      else {
        BigDecimal[] valueArray = new BigDecimal[collection.size()];
        int count = 0, middle = collection.size() / 2;
        BigDecimal medianValue;

        for (SWRLBuiltInArgument element : collection) {
          checkThatElementIsComparable(element);
          BigDecimal value = getArgumentAsADecimal(element);
          valueArray[count++] = value;
        }

        Arrays.sort(valueArray);

        if (collection.size() % 2 == 1)
          medianValue = valueArray[middle];
        else
          medianValue = (valueArray[middle - 1].add(valueArray[middle])).divide(BigDecimal.valueOf(2));

        SWRLBuiltInArgument resultArgument = createLeastNarrowNumericLiteralBuiltInArgument(medianValue,
          new ArrayList<>(collection));

        return processResultArgument(arguments, resultArgumentNumber, resultArgument);
      }
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nth(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments) - 1; // 1-offset for user, 0 for processing

      if (!sortedList.isEmpty()) {
        if (n >= 0 && n < sortedList.size()) {
          SWRLBuiltInArgument nth = sortedList.get(n);
          return processResultArgument(arguments, resultArgumentNumber, nth);
        } else
          return false;
      } else
        return false;
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean greatest(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;

    if (getIsInConsequent())
      return true; // Post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);

      if (!sortedList.isEmpty()) {
        SWRLBuiltInArgument greatest = sortedList.get(sortedList.size() - 1);
        return processResultArgument(arguments, resultArgumentNumber, greatest);
      } else
        return false;
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nthGreatest(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);

      if (!sortedList.isEmpty() && n > 0 && n <= sortedList.size()) {
        SWRLBuiltInArgument nthGreatest = sortedList.get(sortedList.size() - n);
        return processResultArgument(arguments, resultArgumentNumber, nthGreatest);
      } else
        return false;
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean least(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;

    if (getIsInConsequent())
      return true; // Post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);

      if (!sortedList.isEmpty()) {
        SWRLBuiltInArgument least = sortedList.get(resultArgumentNumber);

        return processResultArgument(arguments, resultArgumentNumber, least);
      } else
        return false;
    }
  }

  /*
   * ******************************************************************************************************************
   * SQWRL operators that work with a single collection and return a collection
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNthGreatest(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);

      if (!sortedList.isEmpty() && n > 0 && n <= sortedList.size())
        sortedList.remove(sortedList.size() - n);

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, sortedList);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nthSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, sliceSizeArgumentNumber = 3, numberOfCoreArguments = 4;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments) - 1; // 1-offset for user, 0 for processing
      int sliceSize = convertArgumentToPositiveInt(sliceSizeArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> slice = new ArrayList<>();

      if (!sortedList.isEmpty() && n >= 0) {
        int startIndex = n;
        int finishIndex = n + sliceSize - 1;
        for (int index = startIndex; index <= finishIndex && index < sortedList.size(); index++)
          slice.add(sortedList.get(index));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, slice);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNthSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, sliceSizeArgumentNumber = 3, numberOfCoreArguments = 4;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments) - 1; // 1-offset for user, 0 for processing
      int sliceSize = convertArgumentToPositiveInt(sliceSizeArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> notSlice = new ArrayList<>();

      if (!sortedList.isEmpty() && n >= 0 && n < sortedList.size()) {
        int startIndex = n;
        int finishIndex = n + sliceSize - 1;
        for (int index = 0; index < sortedList.size(); index++)
          if (index < startIndex || index > finishIndex)
            notSlice.add(sortedList.get(index));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, notSlice);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nthGreatestSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, sliceSizeArgumentNumber = 3, numberOfCoreArguments = 4;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> slice = new ArrayList<>();
      int sliceSize = convertArgumentToPositiveInt(sliceSizeArgumentNumber, arguments);

      if (!sortedList.isEmpty() && n > 0) {
        int startIndex = sortedList.size() - n;
        int finishIndex = startIndex + sliceSize - 1;
        if (startIndex < 0)
          startIndex = 0;
        for (int index = startIndex; index <= finishIndex && index < sortedList.size(); index++)
          slice.add(sortedList.get(index));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, slice);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNthGreatestSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, sliceSizeArgumentNumber = 3, numberOfCoreArguments = 4;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);
      int sliceSize = convertArgumentToPositiveInt(sliceSizeArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> slice = new ArrayList<>();

      if (!sortedList.isEmpty() && n > 0 && n <= sortedList.size()) {
        int startIndex = sortedList.size() - n;
        int finishIndex = startIndex + sliceSize - 1;
        for (int index = 0; index < sortedList.size(); index++)
          if (index < startIndex || index > finishIndex)
            slice.add(sortedList.get(index));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, slice);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNth(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments) - 1; // 1-offset for user, 0 for processing

      if (!sortedList.isEmpty() && n >= 0 && n < sortedList.size())
        sortedList.remove(n);

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, sortedList);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notGreatest(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);

      if (!sortedList.isEmpty())
        sortedList.remove(sortedList.size() - 1);

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, sortedList);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean greatestN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> greatestN = new ArrayList<>();

      if (!sortedList.isEmpty() && n > 0) {
        int startIndex = sortedList.size() - n;
        if (startIndex < 0)
          startIndex = 0;
        for (int i = startIndex; i < sortedList.size(); i++)
          greatestN.add(sortedList.get(i));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, greatestN);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notGreatestN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> notGreatestN = new ArrayList<>();

      if (!sortedList.isEmpty() && n > 0) {
        int startIndex = sortedList.size() - n;
        if (startIndex < 0)
          startIndex = 0;
        for (int i = 0; i < startIndex; i++)
          notGreatestN.add(sortedList.get(i));
      }

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, notGreatestN);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notLeast(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, numberOfCoreArguments = 2;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);

      if (!sortedList.isEmpty())
        sortedList.remove(0); // Remove the first (least) element; if there are multiple element with same least value,
      // they will not be removed

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, sortedList);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean leastN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments) - 1;
      List<@NonNull SWRLBuiltInArgument> leastN = new ArrayList<>();

      for (int i = 0; i <= n && i < sortedList.size(); i++)
        leastN.add(sortedList.get(i));

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, leastN);
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notLeastN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    final int resultCollectionArgumentNumber = 0, sourceCollectionArgumentNumber = 1, nArgumentNumber = 2, numberOfCoreArguments = 3;

    if (getIsInConsequent())
      return true; // Non collection operator that is post processed - ignore
    else {
      List<@NonNull SWRLBuiltInArgument> sortedList = getSortedListInSingleOperandCollectionOperation(arguments,
        sourceCollectionArgumentNumber, numberOfCoreArguments);
      int n = convertArgumentToPositiveInt(nArgumentNumber, arguments);
      List<@NonNull SWRLBuiltInArgument> notLeastN = new ArrayList<>();

      for (int i = n; i >= 0 && i < sortedList.size(); i++)
        notLeastN.add(sortedList.get(i));

      return processSingleOperandCollectionOperationListResult(arguments, resultCollectionArgumentNumber,
        sourceCollectionArgumentNumber, numberOfCoreArguments, notLeastN);
    }
  }

  /*
   * ******************************************************************************************************************
   * SQWRL operators that work with two collections and return an element or evaluate to true or false
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean intersects(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int collection1ArgumentNumber = 0, collection2ArgumentNumber = 1, numberOfCoreArguments = 2;
    String queryName = getInvokingRuleName();
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);

    return !Collections.disjoint(collection1, collection2);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notIntersects(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return !intersects(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean contains(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int collection1ArgumentNumber = 0, collection2ArgumentNumber = 1, numberOfCoreArguments = 2;
    String queryName = getInvokingRuleName();
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);

    return collection1.containsAll(collection2);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notContains(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return !contains(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean equal(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int collection1ArgumentNumber = 0, collection2ArgumentNumber = 1, numberOfCoreArguments = 2;
    String queryName = getInvokingRuleName();
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);

    if (collection1GroupKey.equals(collection2GroupKey))
      return true; // The same collection was passed
    else { // Different collections - compare them
      Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
      Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);
      return collection1.equals(collection2); // Remember, sets and lists will not be equal
    }
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notEqual(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    return !equal(arguments);
  }

  /*
   * ******************************************************************************************************************
   * SQWRL operators that work with two collections and return a collection
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean intersection(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultCollectionArgumentNumber = 0, collection1ArgumentNumber = 1, collection2ArgumentNumber = 2, numberOfCoreArguments = 3;
    String queryName = getInvokingRuleName();
    String resultCollectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    int collectionResultNumberOfGroupElements = collection1NumberOfGroupElements + collection2NumberOfGroupElements;
    String resultCollectionGroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      resultCollectionArgumentNumber, numberOfCoreArguments, 0, collectionResultNumberOfGroupElements);
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);
    Collection<SWRLBuiltInArgument> intersection = new HashSet<>(collection1);

    intersection.retainAll(collection2);

    if (!isCollection(queryName, resultCollectionName, resultCollectionGroupKey))
      recordCollection(queryName, resultCollectionName, resultCollectionGroupKey, intersection);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, resultCollectionName, resultCollectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean append(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultCollectionArgumentNumber = 0, collection1ArgumentNumber = 1, collection2ArgumentNumber = 2, numberOfCoreArguments = 3;
    String queryName = getInvokingRuleName();
    String resultCollectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    int resultCollectionNumberOfGroupElements = collection1NumberOfGroupElements + collection2NumberOfGroupElements;
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    String resultCollectionGroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      resultCollectionArgumentNumber, numberOfCoreArguments, 0, resultCollectionNumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);
    List<@NonNull SWRLBuiltInArgument> resultCollection = new ArrayList<>(collection1);

    resultCollection.addAll(collection2);

    if (!isCollectionRecorded(resultCollectionName, resultCollectionGroupKey))
      recordCollection(queryName, resultCollectionName, resultCollectionGroupKey, resultCollection);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, resultCollectionName, resultCollectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean union(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultCollectionArgumentNumber = 0, collection1ArgumentNumber = 1, collection2ArgumentNumber = 2, numberOfCoreArguments = 3;
    String queryName = getInvokingRuleName();
    String resultCollectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    int resultCollectionNumberOfGroupElements = collection1NumberOfGroupElements + collection2NumberOfGroupElements;
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    String resultCollectionGroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      resultCollectionArgumentNumber, numberOfCoreArguments, 0, resultCollectionNumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);
    Set<@NonNull SWRLBuiltInArgument> union = new HashSet<>(collection1);

    union.addAll(collection2);

    if (!isCollection(queryName, resultCollectionName, resultCollectionGroupKey))
      recordCollection(queryName, resultCollectionName, resultCollectionGroupKey, union);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, resultCollectionName, resultCollectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean difference(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    checkThatInAntecedent();

    final int resultCollectionArgumentNumber = 0, collection1ArgumentNumber = 1, collection2ArgumentNumber = 2, numberOfCoreArguments = 3;
    String queryName = getInvokingRuleName();
    String resultCollectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String collection1Name = getCollectionName(arguments, collection1ArgumentNumber);
    String collection2Name = getCollectionName(arguments, collection2ArgumentNumber);
    int collection1NumberOfGroupElements = getNumberOfGroupElements(queryName, collection1Name);
    int collection2NumberOfGroupElements = getNumberOfGroupElements(queryName, collection2Name);
    int collectionResultNumberOfGroupElements = collection1NumberOfGroupElements + collection2NumberOfGroupElements;
    String resultCollectionGroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      resultCollectionArgumentNumber, numberOfCoreArguments, 0, collectionResultNumberOfGroupElements);
    String collection1GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection1ArgumentNumber, numberOfCoreArguments, 0, collection1NumberOfGroupElements);
    String collection2GroupKey = getCollectionGroupKeyInMultiOperandCollectionOperation(arguments,
      collection2ArgumentNumber, numberOfCoreArguments, collection1NumberOfGroupElements,
      collection2NumberOfGroupElements);
    Collection<SWRLBuiltInArgument> collection1 = getCollection(queryName, collection1Name, collection1GroupKey);
    Collection<SWRLBuiltInArgument> collection2 = getCollection(queryName, collection2Name, collection2GroupKey);
    Collection<SWRLBuiltInArgument> difference = new HashSet<>(collection1);

    difference.removeAll(collection2);

    if (!isCollection(queryName, resultCollectionName, resultCollectionGroupKey))
      recordCollection(queryName, resultCollectionName, resultCollectionGroupKey, difference);

    if (isUnboundArgument(resultCollectionArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultCollectionArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, resultCollectionName, resultCollectionGroupKey);
      variableArgument.setBuiltInResult(collectionArgument);
    }

    return true;
  }

  /*
   * ******************************************************************************************************************
   * Alias definitions for SQWRL operators
   * *******************************************************************************************************************
   */

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nthLast(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return nthGreatest(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNthLast(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notNthGreatest(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean nthLastSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return nthGreatestSlice(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notNthLastSlice(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notNthGreatestSlice(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean last(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return greatest(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notLast(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notGreatest(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean lastN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return greatestN(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notLastN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notGreatestN(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean first(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return least(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notFirst(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notLeast(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean firstN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return leastN(arguments);
  }

  /**
   * @param arguments A list of SWRL built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an error occurs during processing
   */
  public boolean notFirstN(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    return notLeastN(arguments);
  }

  /*
   * ******************************************************************************************************************
   * Internal methods
   * *******************************************************************************************************************
   */

  @NonNull private String getCollectionGroupKeyInMake(@NonNull List<@NonNull SWRLBuiltInArgument> arguments)
    throws SWRLBuiltInException
  {
    // The collection is the first argument, the element is the second; subsequent arguments (if any) are group elements
    final int collectionArgumentNumber = 0, numberOfCoreArguments = 2;
    checkNumberOfArgumentsAtLeast(numberOfCoreArguments, arguments.size());

    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, collectionArgumentNumber);
    String collectionKey = createCollectionKey(queryName, collectionName);
    int numberOfGroupArguments = arguments.size() - numberOfCoreArguments;
    boolean hasGroupPattern = numberOfGroupArguments != 0;
    String groupPattern = !hasGroupPattern ?
      "" :
      createInvocationPattern(getBuiltInBridge(), queryName, 0, false, arguments.subList(2, arguments.size()));

    if (isBoundArgument(collectionArgumentNumber, arguments) && !this.collectionGroupElementNumbersMap
      .containsKey(collectionKey)) {
      // Collection variable already used in non collection context
      throw new SWRLBuiltInException(
        "collection variable ?" + arguments.get(collectionArgumentNumber).asVariable().getVariableName()
          + " already used in non collection context in query " + queryName);
    }

    if (hasGroupPattern) {
      if (!this.collectionGroupElementNumbersMap.containsKey(collectionKey))
        this.collectionGroupElementNumbersMap.put(collectionKey, numberOfGroupArguments);
      else if (this.collectionGroupElementNumbersMap.get(collectionKey) != numberOfGroupArguments) {
        throw new SWRLBuiltInException(
          "internal error: inconsistent number of group elements for collection " + collectionName + " in query "
            + queryName);
      }
      return groupPattern;
    } else {
      if (this.collectionGroupElementNumbersMap.containsKey(collectionKey)) {
        if (this.collectionGroupElementNumbersMap.get(collectionKey) != 0) {
          throw new SWRLBuiltInException(
            "internal error: inconsistent number of group elements for collection " + collectionName + " in query "
              + queryName);
        }
      } else
        this.collectionGroupElementNumbersMap.put(collectionKey, 0);
      return "";
    }
  }

  private String getCollectionGroupKeyInSingleCollectionOperation(@NonNull List<@NonNull SWRLBuiltInArgument> arguments,
    int coreNumberOfArguments) throws SWRLBuiltInException
  {
    String queryName = getInvokingRuleName();

    checkThatInAntecedent();

    if ((arguments.size() > coreNumberOfArguments)) // Is a grouped collection
      return createInvocationPattern(getBuiltInBridge(), queryName, 0, false,
        arguments.subList(coreNumberOfArguments, arguments.size()));
    else
      return "";
  }

  private String getCollectionGroupKeyInMultiOperandCollectionOperation(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int collectionArgumentNumber, int coreNumberOfArguments,
    int groupArgumentOffset, int numberOfRelevantGroupArguments) throws SWRLBuiltInException
  {
    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, collectionArgumentNumber);
    String collectionKey = createCollectionKey(queryName, collectionName);

    checkThatInAntecedent();

    if (!this.collectionGroupElementNumbersMap.containsKey(collectionKey))
      this.collectionGroupElementNumbersMap.put(collectionKey, numberOfRelevantGroupArguments);

    if (numberOfRelevantGroupArguments != 0) // Is a grouped collection
      return createInvocationPattern(getBuiltInBridge(), queryName, 0, false, arguments
        .subList(coreNumberOfArguments + groupArgumentOffset,
          coreNumberOfArguments + groupArgumentOffset + numberOfRelevantGroupArguments));
    else
      return "";
  }

  private boolean processSingleOperandCollectionOperationListResult(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int resultCollectionArgumentNumber,
    int sourceCollectionArgumentNumber, int numberOfCoreArguments, Collection<SWRLBuiltInArgument> resultList)
    throws SWRLBuiltInException
  {
    String queryName = getInvokingRuleName();
    String sourceCollectionName = getCollectionName(arguments, sourceCollectionArgumentNumber);
    String resultCollectionName = getCollectionName(arguments, resultCollectionArgumentNumber);
    String resultCollectionGroupKey = getCollectionGroupKeyInSingleCollectionOperation(arguments,
      numberOfCoreArguments);
    String resultCollectionKey = createCollectionKey(queryName, resultCollectionName);

    if (!isCollection(queryName, resultCollectionName, resultCollectionGroupKey))
      recordCollection(queryName, resultCollectionName, resultCollectionGroupKey, resultList);

    if (!this.collectionGroupElementNumbersMap.containsKey(resultCollectionKey)) // Give it the same number of group
      // elements as the source collection
      this.collectionGroupElementNumbersMap
        .put(resultCollectionKey, getNumberOfGroupElements(queryName, sourceCollectionName));

    return processListResultArgument(arguments, resultCollectionArgumentNumber, resultCollectionName,
      resultCollectionGroupKey, resultList);
  }

  private boolean processListResultArgument(@NonNull List<@NonNull SWRLBuiltInArgument> arguments,
    int resultArgumentNumber, @NonNull String resultListName, @NonNull String resultListID,
    @NonNull Collection<SWRLBuiltInArgument> resultList) throws SWRLBuiltInException
  {
    checkArgumentNumber(resultArgumentNumber, arguments);

    String queryName = getInvokingRuleName();

    if (isUnboundArgument(resultArgumentNumber, arguments)) {
      SWRLVariableBuiltInArgument variableArgument = arguments.get(resultArgumentNumber).asVariable();
      IRI variableIRI = variableArgument.getIRI();

      SQWRLCollectionVariableBuiltInArgument collectionArgument = createSQWRLCollectionVariableBuiltInArgument(
        variableIRI, queryName, resultListName, resultListID);
      variableArgument.setBuiltInResult(collectionArgument);

      return true;
    } else {
      Collection<SWRLBuiltInArgument> collection = getCollection(queryName, resultListName, resultListID);
      return collection.equals(resultList); // Remember, sets and lists will not be equal
    }
  }

  private SQWRLResultGenerator getSQWRLResultGenerator(@NonNull String queryName) throws SWRLBuiltInException
  {
    return getBuiltInBridge().getSQWRLResultGenerator(queryName);
  }

  private void checkThatElementIsComparable(@NonNull SWRLBuiltInArgument argument) throws SWRLBuiltInException
  {
    if (argument instanceof SWRLLiteralBuiltInArgument) {
      SWRLLiteralBuiltInArgument literalArgument = (SWRLLiteralBuiltInArgument)argument;
      SQWRLLiteralResultValue literal = getSQWRLResultValueFactory().getLiteralValue(literalArgument.getLiteral());
      if (!literal.isComparable())
        throw new SWRLBuiltInException("may only be applied to collections with comparable elements");
    } else
      throw new SWRLBuiltInException("may only be applied to collections with comparable elements");
  }

  private Collection<SWRLBuiltInArgument> getCollectionInSingleCollectionOperation(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int sourceCollectionArgumentNumber,
    int coreNumberOfArguments) throws SWRLBuiltInException
  {
    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, sourceCollectionArgumentNumber);
    String collectionGroupKey = getCollectionGroupKeyInSingleCollectionOperation(arguments, coreNumberOfArguments);

    return getCollection(queryName, collectionName, collectionGroupKey);
  }

  @NonNull private List<@NonNull SWRLBuiltInArgument> getSortedListInSingleOperandCollectionOperation(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int sourceCollectionArgumentNumber,
    int coreNumberOfArguments) throws SWRLBuiltInException
  {
    String queryName = getInvokingRuleName();
    String collectionName = getCollectionName(arguments, sourceCollectionArgumentNumber);
    String collectionGroupKey = getCollectionGroupKeyInSingleCollectionOperation(arguments, coreNumberOfArguments);

    return getSortedList(queryName, collectionName, collectionGroupKey);
  }

  // We do not cache because only one built-in will typically perform an operation on a particular collection per query.
  // Note: currently implementations may modify the returned collection.
  @NonNull private List<@NonNull SWRLBuiltInArgument> getSortedList(@NonNull String queryName,
    @NonNull String collectionName, @NonNull String collectionGroupKey) throws SWRLBuiltInException
  {
    Collection<SWRLBuiltInArgument> collection = getCollection(queryName, collectionName, collectionGroupKey);
    List<@NonNull SWRLBuiltInArgument> result = new ArrayList<>(collection);
    Collections.sort(result); // TODO See if we can be clever (though types not necessarily comparable)

    return result;
  }

  @NonNull private List<@NonNull SWRLBuiltInArgument> createBag(@NonNull String queryName,
    @NonNull String collectionName, @NonNull String collectionGroupKey) throws SWRLBuiltInException
  {
    List<@NonNull SWRLBuiltInArgument> bag = new ArrayList<>();

    recordCollection(queryName, collectionName, collectionGroupKey, bag);

    return bag;
  }

  @NonNull private Set<@NonNull SWRLBuiltInArgument> createSet(@NonNull String queryName,
    @NonNull String collectionName, @NonNull String collectionGroupKey) throws SWRLBuiltInException
  {
    Set<@NonNull SWRLBuiltInArgument> set = new HashSet<>();

    recordCollection(queryName, collectionName, collectionGroupKey, set);

    return set;
  }

  @NonNull private String getCollectionName(@NonNull List<@NonNull SWRLBuiltInArgument> arguments,
    int collectionArgumentNumber) throws SWRLBuiltInException
  {
    return getVariableName(collectionArgumentNumber, arguments);
  }

  private int getNumberOfGroupElements(@NonNull String queryName, @NonNull String collectionName)
    throws SWRLBuiltInException
  {
    String collectionKey = createCollectionKey(queryName, collectionName);

    if (!this.collectionGroupElementNumbersMap.containsKey(collectionKey))
      throw new SWRLBuiltInException(
        "internal error: invalid collection name " + collectionName + " in query " + queryName
          + "; no group element number found");

    return this.collectionGroupElementNumbersMap.get(collectionKey);
  }

  // An ungrouped collection will have a collectionGroupKey of the empty string so will not be partitioned.
  private void recordCollection(@NonNull String queryName, @NonNull String collectionName,
    @NonNull String collectionGroupKey, @NonNull Collection<SWRLBuiltInArgument> collection) throws SWRLBuiltInException
  {
    String collectionKey = createCollectionKey(queryName, collectionName);

    if (!isCollectionRecorded(queryName, collectionName)) { // Is the collection recorded?
      if (isBag(collection))
        this.bagKeys.add(collectionKey);
      else if (isSet(collection))
        this.setKeys.add(collectionKey);
      else
        throw new SWRLBuiltInException(
          "internal error: collection " + collectionName + " in query " + queryName + " with group key "
            + collectionGroupKey + " is neither a bag or a set");

      this.collectionsMap.put(collectionKey, new HashMap<>());
    }

    if (!isCollection(queryName, collectionName, collectionGroupKey)) { // Is the collection for this group recorded
      if (isBag(queryName, collectionName) && !isBag(collection))
        throw new SWRLBuiltInException(
          "attempt to add non bag elements to bag " + collectionName + " in query " + queryName + "; group key="
            + collectionGroupKey);

      if (isSet(queryName, collectionName) && !isSet(collection))
        throw new SWRLBuiltInException(
          "attempt to add non set elements to set " + collectionName + " in query " + queryName + "; group key="
            + collectionGroupKey);

      if (this.collectionsMap.containsKey(collectionKey))
        this.collectionsMap.get(collectionKey).put(collectionGroupKey, collection);
      else
        throw new SWRLBuiltInException(
          "internal error attempting to add non set elements to set " + collectionName + " in query " + queryName
            + "; group key=" + collectionGroupKey);
    }
  }

  @NonNull private Collection<@NonNull SWRLBuiltInArgument> getCollection(@NonNull String queryName,
    @NonNull String collectionName, @NonNull String collectionGroupKey) throws SWRLBuiltInException
  {
    String collectionKey = queryName + ":" + collectionName;

    if (!isCollection(queryName, collectionName, collectionGroupKey))
      throw new SWRLBuiltInException(
        "collection argument in query " + queryName + " with name " + collectionName + " and group key "
          + collectionGroupKey + " does not refer to a collection");

    if (this.collectionsMap.get(collectionKey) != null
      && this.collectionsMap.get(collectionKey).get(collectionGroupKey) != null)
      return this.collectionsMap.get(collectionKey).get(collectionGroupKey);
    else
      throw new SWRLBuiltInException(
        "internal error processing collection argument in query " + queryName + " with name " + collectionName
          + " and group key " + collectionGroupKey);
  }

  private boolean isCollection(@NonNull String queryName, @NonNull String collectionName,
    @NonNull String collectionGroupKey)
  {
    String collectionKey = createCollectionKey(queryName, collectionName);

    return this.collectionsMap.get(collectionKey) != null && this.collectionsMap.get(collectionKey)
      .containsKey(collectionGroupKey);
  }

  private boolean isSet(@NonNull String queryName, @NonNull String collectionName)
  {
    String setKey = createCollectionKey(queryName, collectionName);

    return this.setKeys.contains(setKey);
  }

  private boolean isBag(@NonNull String queryName, @NonNull String collectionName)
  {
    String bagKey = createCollectionKey(queryName, collectionName);

    return this.bagKeys.contains(bagKey);
  }

  private boolean isCollectionRecorded(@NonNull String queryName, @NonNull String collectionName)
  {
    String collectionKey = createCollectionKey(queryName, collectionName);

    return this.collectionsMap.containsKey(collectionKey);
  }

  private boolean isBag(Collection<@NonNull SWRLBuiltInArgument> collection)
  {
    return (collection instanceof List<?>);
  }

  private boolean isSet(Collection<@NonNull SWRLBuiltInArgument> collection)
  {
    return (collection instanceof Set<?>);
  }

  @NonNull private String createCollectionKey(@NonNull String queryName, @NonNull String collectionName)
  {
    return queryName + ":" + collectionName;
  }

  @NonNull @SuppressWarnings("unused") private Collection<@NonNull SWRLBuiltInArgument> ungroupCollection(
    @NonNull String queryName, @NonNull String collectionName) throws SWRLBuiltInException
  {
    if (!isCollectionRecorded(queryName, collectionName))
      throw new SWRLBuiltInException(collectionName + " in query " + queryName + " is not a collection");
    else {
      Collection<@NonNull SWRLBuiltInArgument> ungroupedCollection = isSet(queryName, collectionName) ?
        new HashSet<>() :
        new ArrayList<>();
      String collectionKey = createCollectionKey(queryName, collectionName);

      if (this.collectionsMap.get(collectionKey) != null) {
        for (String collectionGroupKey : this.collectionsMap.get(collectionKey).keySet()) {
          if (this.collectionsMap.get(collectionKey).get(collectionGroupKey) != null)
            ungroupedCollection.addAll(this.collectionsMap.get(collectionKey).get(collectionGroupKey));
        }
      }
      return ungroupedCollection;
    }
  }

  private int convertArgumentToPositiveInt(int argumentNumber, @NonNull List<@NonNull SWRLBuiltInArgument> arguments)
    throws SWRLBuiltInException
  {
    BigInteger integerValue = getArgumentAsAnInteger(argumentNumber, arguments);

    if (integerValue.compareTo(BigInteger.ZERO) < 0)
      throw new InvalidSWRLBuiltInArgumentException(argumentNumber,
        makeInvalidArgumentTypeMessage(arguments.get(argumentNumber), "expecting positive xsd:integer"));

    if (integerValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
      throw new InvalidSWRLBuiltInArgumentException(argumentNumber,
        makeInvalidArgumentTypeMessage(arguments.get(argumentNumber),
          "value converted to xsd:int cannot be larger than " + Integer.MAX_VALUE));

    return integerValue.intValue();
  }

}
