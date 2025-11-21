## I.Workflow GitHub Actions
## 1.Objectif

- Compiler le projet GraphHopper pour plusieurs versions de Java (24 et 25-ea).
- Exécuter tous les tests unitaires.
- Rickroll en cas d’échec des tests
- Exécuter des tests de mutation avec Pitest sur le module core.
- Mettre à jour automatiquement la baseline de mutation si le score s’améliore.

## 2.Structure et choix de conception
- Permission pour écrire, pour stocker automatiquement le meilleur score de mutation

- Test sur plusieurs versions de Java pour assurer la compatibilité(24 et 25e). fail-fast: false pour continuer les builds même si une version échoue.

- Ancien score de mutation stocke dans le fichier Mutation-baseline.txt, s'il n'existe pas le score est mi a 0.

- Lancement de pitest sur le module Core(celui utiliser pour le devoir), et de la commande "tee" pour afficher les log de pitest.

- Utilisation des logs de pitest et de la commande grep pour récupérer le nouveaux score de mutation. qui sera ensuite comparer a l'ancien.

- Si il est supérieur le build ce termine et le fichier baseline est mis a jours sinon le build échoue, elle est mis a jour a l'aide d'un bot git-hub qui ajoutera les changement a la liste et fera un commit de celui ci puis un pus vers la branche master.

### 3. Rick-roll

- Si nous detectons un test échouer, la tache "Test failed diagnostic" se lance et affiche un faux diagnostique affichant des paroles de rickroll et un lien vers la video:

`Run echo "Tests failed! Commit: https://www.youtube.com/watch?v=dQw4w9WgXcQ by Rick"
Tests failed! Commit: https://www.youtube.com/watch?v=dQw4w9WgXcQ by Rick
Take a deep breath...
Never gonna give you up!
Error: Process completed with exit code 1.`

## II. Tests Mockito 
## 1. classes selectionnees

### A. CHPathCalculator

Path: com.graphhopper.routing.CHPathCalculator

**Raison d'avoir choisi cette classe:**
- classe centrale au système de routage
- contient logique de construction et de validation
- Dependances injectables et bien definies

### B.FlexiblePathCalculator

Path: com.graphhopper.routing.FlexiblePathCalculator

**Raison d'avoir choisi cette classe:**
- orchestration complexe et dependances multiples
- gestion d'etats tres interessante a tester
- valide l'implementation de l'interface d'algo a l'execution

## 2. Dependance ajoutees:

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```
On a ajoute les dependances Mockito  dans le fichier 'core/pom.xml'

## 3. Classes simulees avec Mockito

### A. CHPathCalculator:
**CHRoutingAlgorithmFactory** : fabrique chargee de creer des algos CH, simulee pour controler quel algo est instancie durant le test.
**EdgeToEdgeRoutingAlgorithm** : algo CH qui calcule les chemins, simulee pour controler le comportement de l'algo


### B.FlexiblePathCalculator
**QueryGraph**: graphe, simulee pour controler l'etat du  graphe sans avoir a construire un vrai reseau routier
**RoutingAlgorithmFactory** : fabrique chargee de creer algos flexibles, simulee pour choisir l'algo de routage de maniere deterministe
**RoutingAlgorithm** : algo routage utilisee,  simulee pour controler le calcul du chemin
**Path** : objet chemin calculee, simulee pour retourner un resultat de chemin previsible

## 4. Tests

### A. CHPathCalculatorMockitoTest

- **testRejectsUnfavouredEdgesInCH**: test de rejet des aretes non favorisees
- **testDetectionEmptyPathList**: test de detection d'une liste de chemins vide

### B. FlexiblePathCalculatorMockitoTest

- **testNonBidirectionalWithEdgeRestrictionsAlgo**: test algo non bidirectionnel avec restrictions d'aretes
- **testMaxVisitedNodesExceedLimit**: Depassement du nombre max de noeuds visites



## 5. Details

### A. CHPathCalculator

### 1.testRejectsUnfavouredEdgesInCH

Test rejet des arêtes défavorisées en mode CH.

1. classes testees:
CHPathCalculator: Validation que reject les cas non supportés

2. classes simulees:
- CHRoutingAlgorithmFactory: simule une fabrique qui ne doit jamais être appelée
Ce test valide que la validation est faite avant la création de l'algo.

3.definition des mocks:
- algoFactoryMock: fabrique simulée qui ne doit jamais être appelée,  et cela demontre que la validation est faite avant la création de l'algo.

4.choix valeurs simulées:
- Arêtes défavorisées: 100, 101, 102, 3 arêtes arbitraires  et distinctes pour montrer que la liste est non vide

- Noeuds source/cible: 50, 60, valeurs choisies au hasard representant une requête normale


### 2.testDetectionEmptyPathList

Test détection d'une liste de chemins vide (cas d'erreur)

1. classes testees:

CHPathCalculator: verifie qu'une liste de chemin vide (resultat invalide) est refusée au lieu d'être propagée.

2. classes simulees:

- CHRoutingAlgorithmFactory: Simule fabrique algo

- EdgeToEdgeRoutingAlgorithm: Simule un algo bugué qui renvoi une liste vide plutot que de lever une exception ou retourner un chemin

3. definition des mocks:
- algoFactoryMock: configure pour renvoyer algoMock quand createAlgo est appelé, controle complet de l'algo utilise.

- algoMock: simule un algo bugué qui retourne une liste vide de chemins.

4. choix valeurs simulées:
- Noeuds sources/cibles: 30, 40, noeuds qui ont des Valeurs chosies au hasard  mais distinctes, designant une requete normale.

- Retour de calcPaths: Collections.emptyList(), comportement invalide qu'on teste explicitement pour verifier la detection d'un invariant violé.


### B. FlexiblePathCalculatorMockitoTest

### 1.testNonBidirectionalWithEdgeRestrictionsAlgo

Test validation d'interface/ contrat : algo doit être bidirectionnel avec arêtes

Ce test refuse un algo non edge to edge quand des restrictions d'aretes sont fournies.

1. classes testees:
- FlexiblePathCalculator: Validation de contrat d'algo

2. classes simulees:
- QueryGraph: Simule réseau. Moqué pour isoler le test de GraphHopper

- RoutingAlgorithmFactory: Fabrique qui renvoie un algo incompatible. Moquée pour tester le path d'erreur sans créer une vraie fabrique.

- RoutingAlgorithm: Algo generique sans capacité arête-à-arête. Moqué pour simuler un algo qui n'hérite pas de EdgeToEdgeRoutingAlgorithm. 

3. définition des mocks:
- algoFactoryMock: pour isoler le test. Retourne expres un algo incompatible (algoMock) pour creer un algo de routage

- algoMock: designe un algo de routage générique qui ne supporte pas  interface edge to edge. Son nom simulee identifie un algo incompatible, ce qui a pour but de violer le contrat et de lever l'exception.

4. choix des Valeurss simulees:
 - Noeuds source/cible: 10, 20; noeuds qui ont des Valeurs chosies au hasard  mais sont des nœuds valides, et montre que l'erreur est détectée avant calcul.

- Arête source: 50; declenche logique de restriction d'arête

- Nom algorithme: "NonBidirectionalAlgorithm", Identifie le type d'algo incompatible


### 2.testMaxVisitedNodesExceedLimit

Test dépassement du nombre maximal de noeuds visités


1. classes testees:
- FlexiblePathCalculator: rejete resultat ou le nombre de noeuds visités depasse la limite de maxVisitedNodes

2.classes simulees:
- QueryGraph: Simule le réseau. Moqué pour isoler de la complexité du graphe réel.

- RoutingAlgorithmFactory: Fabrique qui crée l'algorithme avec la limite maxVisitedNodes. Moquée pour forcer l'utilisation de l'algo simulé algoMock et eviter utilisation de la fabrique réelle.

- RoutingAlgorithm: Algorithme qui retourne un résultat mais avec trop de noeuds visités. Moqué pour simuler un cas où l'algorithme visite beaucoup de noeuds.

- Path: simule pour permettre a calcPaths de retourner une liste non vide.
     
3.Définition des mocks:
- algoFactoryMock: force fabrique a renvoyer algoMock avec les options spécifiées

- algoMock: simule algo normal qui calcule un chemin mais visite trop de noeuds. Retourne une liste de chemins valide mais avec un nombre de noeuds visités 'getVisitedNodes()' supérieur à la limite.

- pathMock: simule un chemin valide pour que calcPaths puisse retourner une liste non vide.
     
4.choix des Valeurss simulees:
- MaxVisitedNodes: 10000; Limite intentionnellement stricte pour ce test

- getVisitedNodes(): 50000, valeur considerablement  superieure (5x la limite) pour lever l'exception

- Noeuds source/cible: 100, 200, valeurs chosies au hasard mais restent des valeurs valides

## 6. resultats

Tous les tests passent avec succes
