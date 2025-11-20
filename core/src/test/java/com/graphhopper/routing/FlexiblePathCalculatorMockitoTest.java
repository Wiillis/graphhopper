/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.routing;

import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.exceptions.MaximumNodesExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour FlexiblePathCalculator avec mocks Mockito
 *
 * On teste le calculateur de chemins flexible et on a:
 * - QueryGraph: graphe comportant le réseau routier
 * - RoutingAlgorithmFactory: fabrique d'algo
 * - Weighting: fonction coût pour les arêtes
 * - RoutingAlgorithm: algorithme routage
 * - Path: chemin calculé par algo
 *
 * Les classes simulees sont:
 * QueryGraph
 * RoutingAlgorithmFactory
 * RoutingAlgorithm
 * Path
 */



// certaines parties du code ont été générées à l’aide de GitHub Copilot
// certaines parties des commentaires ont été rédigés par GitHub Copilot

// extension Mockito
@ExtendWith(MockitoExtension.class)
public class FlexiblePathCalculatorMockitoTest {

    // graphe simulé pour isoler le calculateur du graphe réel
    @Mock
    private QueryGraph queryGraphMock;
    
    // fabrique d'algo simulée, controle complet de l'algo créé
    @Mock
    private RoutingAlgorithmFactory algoFactoryMock;

    // algo routage simulé 
    @Mock
    private RoutingAlgorithm algoMock;
    
    // fonction de coût simulée
    @Mock
    private Weighting weightFunctionMock;

    // chemin simulé pour permettre le retour d'une liste non vide
    @Mock
    private Path pathMock;
    
    // objet calculateur testé
    private FlexiblePathCalculator calculator;

    // options d'algo utilisées 
    private AlgorithmOptions algoOptions;
    
    @BeforeEach
    public void setUp() {
        // initialisation des options d'algo
        algoOptions = new AlgorithmOptions();
        algoOptions.setMaxVisitedNodes(Integer.MAX_VALUE);
    }


    /**
     * Test validation d'interface/ contrat : algo doit être bidirectionnel avec arêtes
     *
     * Ce test refuse un algo non edge to edge quand des restrictions d'aretes sont fournies.
     *
     * classes testees:
     * - FlexiblePathCalculator: Validation de contrat d'algo
     *
     * classes simulees:
     * - QueryGraph: Simule réseau. Moqué pour isoler le test de GraphHopper
     * - RoutingAlgorithmFactory: Fabrique qui renvoie un algo incompatible. Moquée pour tester le path d'erreur sans créer une vraie fabrique.
     * - RoutingAlgorithm: Algo generique sans capacité arête-à-arête. Moqué pour simuler un algo qui n'hérite pas de EdgeToEdgeRoutingAlgorithm. 
     *
     * définition des mocks:
     * - algoFactoryMock: pour isoler le test. Retourne expres un algo incompatible (algoMock) pour creer un algo de routage
     * - algoMock: designe un algo de routage générique qui ne supporte pas  interface edge to edge. Son nom simulee identifie un algo incompatible, ce qui a pour but de violer le contrat et de lever l'exception.
     * 
     * choix des Valeurss simulees:
     * - Noeuds source/cible: 10, 20; noeuds qui ont des Valeurs chosies au hasard  mais sont des nœuds valides, et montre que l'erreur est détectée avant calcul.
     * - Arête source: 50; declenche logique de restriction d'arête
     * - Nom algorithme: "NonBidirectionalAlgorithm", Identifie le type d'algo incompatible
     */

    @Test
    public void testNonBidirectionalWithEdgeRestrictionsAlgo() {

        // fabrique renvoi algo incompatible
        when(algoFactoryMock.createAlgo(queryGraphMock, weightFunctionMock, algoOptions))
                .thenReturn(algoMock);
        when(algoMock.getName()).thenReturn("NonBidirectionalAlgorithm");

        // creer le calculateur avec les mocks
        calculator = new FlexiblePathCalculator(queryGraphMock, algoFactoryMock, weightFunctionMock, algoOptions);

        // definir restrictions d'aretes
        EdgeRestrictions restrictions = new EdgeRestrictions();
        restrictions.setSourceOutEdge(50);

        // Exception attendue car algo est incompatible avec les restrictions d'aretes
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calcPaths(10, 20, restrictions)
        );
    }

    /**
     * Test dépassement du nombre maximal de noeuds visités
     *
     * classes testees:
     * - FlexiblePathCalculator: rejete resultat ou le nombre de
     * noeuds visités depasse la limite de maxVisitedNodes
     *
     * classes simulees:
     * - QueryGraph: Simule le réseau. Moqué pour isoler de la complexité du graphe réel.
     * - RoutingAlgorithmFactory: Fabrique qui crée l'algorithme avec la
     *   limite maxVisitedNodes. Moquée pour forcer l'utilisation de l'algo simulé algoMock et eviter utilisation de la fabrique réelle.
     * - RoutingAlgorithm: Algorithme qui retourne un résultat mais avec
     *   trop de noeuds visités. Moqué pour simuler un cas où l'algorithme visite beaucoup de noeuds.
     * - Path: simule pour permettre a calcPaths de retourner une liste non vide.
     
     * Définition des mocks:
     * - algoFactoryMock: force fabrique a renvoyer algoMock avec les options spécifiées
     * - algoMock: simule algo normal qui calcule un chemin mais visite trop de noeuds. Retourne une liste de chemins valide mais avec un nombre de noeuds visités 'getVisitedNodes()' supérieur à la limite.
     * - pathMock: simule un chemin valide pour que calcPaths puisse retourner une liste non vide.
     
     * choix des Valeurss simulees:
     * - MaxVisitedNodes: 10000; Limite intentionnellement stricte pour ce test
     * - getVisitedNodes(): 50000, valeur considerablement  superieure (5x la limite) pour lever l'exception
     * - Noeuds source/cible: 100, 200, valeurs chosies au hasard mais restent des valeurs valides
     */
    @Test
    public void testMaxVisitedNodesExceedLimit(){

        // Fixe limite max de noeuds visités et configure les mocks
        algoOptions.setMaxVisitedNodes(10000);

        // fabrique renvoie l'algo simulé
        when(algoFactoryMock.createAlgo(queryGraphMock, weightFunctionMock, algoOptions))
                .thenReturn(algoMock);
        // simuler que l'algo renvoi une liste de chemins non vide
        when(algoMock.calcPaths(anyInt(), anyInt())).thenReturn(Collections.singletonList(pathMock));
        
        // algo simule d'avoir visite 50000 noeuds, depassant la limite
        when(algoMock.getVisitedNodes()).thenReturn(50000);

        // creer le calculateur avec les mocks
        calculator = new FlexiblePathCalculator(queryGraphMock, algoFactoryMock, weightFunctionMock, algoOptions);
        EdgeRestrictions restrictions = new EdgeRestrictions();

        // Exception attendue car limite depassee
        assertThrows(
                MaximumNodesExceededException.class,
                () -> calculator.calcPaths(100, 200, restrictions)
        );
    }
}