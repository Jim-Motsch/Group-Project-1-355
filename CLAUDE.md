## Setup
- Java 17, Maven (add dependencies only when a rule needs them, e.g. H2 for IDS00-J)
- Single package: src/main/java/cert
- One class per rule, named after the rule code (e.g. Err08J.java)
- The rule-to-member assignments are in RULES.md; put the member's name in each class's JavaDoc
- Each member has 5 rules (25 total), confirmed; RULES.md is correct as is

## Conventions
- Every rule class contains BOTH a noncompliant and a compliant version, clearly labeled (e.g. methods named noncompliant()/compliant() with JavaDoc saying which is which)
- Recommendation classes are named by their real CERT code (e.g. Obj50J.java), not Rec05J01
- The integration class is IntegratedDemo.java; it combines as many rules and recommendations as possible, with detailed comments
- All code uses JavaDoc-style comments
- Every demo's output must visibly differ between the noncompliant and compliant versions; if the output is identical, rework the demo until it differs

## Grading (for context)
- Report: 150 pts = 80 combined example, 50 rules, 10 recommendations, 10 formatting
- Presentation: 20 intro/overview, 10 conclusion + time limit, 20 demo