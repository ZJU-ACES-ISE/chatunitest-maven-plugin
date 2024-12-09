Information of the focal method is
```${full_fm}```.

Following the listed rules:
1.Insufficient Test Coverage: If the tests do not cover the mutated code paths sufficiently, PIT won't be able to detect the mutation. This often happens if the tests miss corner cases or specific conditional branches in the mutated code.

2.Poor Test Case Design: Even if test coverage is high, poorly designed test cases with insufficient assertions or validations may fail to catch the faults introduced by the mutations. Proper assertions should validate the expected behavior thoroughly.

3.Equivalent Mutants: Sometimes the mutation doesn't introduce a real fault because it doesn't affect the program's observable behavior. These are called "equivalent mutants," and they are inherently undetectable by any test suite.

Please refine the unit test to get higher mutation score. You can use Junit 5, Mockito 3 and reflection. No explanation is needed.