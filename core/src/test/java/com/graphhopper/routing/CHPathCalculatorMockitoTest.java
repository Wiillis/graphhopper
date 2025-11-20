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

import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.routing.ch.CHRoutingAlgorithmFactory;
import com.graphhopper.util.PMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CHPathCalculator avec mocks Mockito
 *
 * Ces tests valident le  comportement du calculateur de chemins basé sur
 * les hiérarchies de contraction. On a:
 * - CHRoutingAlgorithmFactory: fabrique pour créer des algos CH
 * - EdgeToEdgeRoutingAlgorithm: algo qui exécute le calcul de chemin en mode CH
 * - Path: chemin calculé par l'algo
 *
 * Classes simulées avec Mockito:
 * CHRoutingAlgorithmFactory
 * EdgeToEdgeRoutingAlgorithm
 */

// certaines parties du code ont été générées à l’aide de GitHub Copilot
// certaines parties des commentaires ont été rédigés par GitHub Copilot

// extension Mockito
@ExtendWith(MockitoExtension.class)
public class CHPathCalculatorMockitoTest {

    // fabrique simulée pour eviter la création reelle d'un algo CH
    @Mock
    private CHRoutingAlgorithmFactory algoFactoryMock;

    // algo CH simulé afin de contrôler le comportement de l'algo utilisé
    @Mock
    private EdgeToEdgeRoutingAlgorithm algoMock;

    // objet calculateur testé
    private CHPathCalculator calculator;
    // options routage utilisées par le calculateur
    private PMap algoOptions;

    @BeforeEach
    public void setUp() {
        // initialisation des options d'algo
        algoOptions = new PMap();
    }



    /**
     * Test rejet des arêtes défavorisées en mode CH.
     *
     * classes testees:
     * - CHPathCalculator: Validation que reject les cas non supportés
     *
     * classes simulees:
     * - CHRoutingAlgorithmFactory: simule une fabrique qui ne doit jamais être appelée
     *   Ce test valide que la validation est faite avant la création de l'algo.
     *
     * definition des mocks:
     * - algoFactoryMock: fabrique simulée qui ne doit jamais être appelée,  et cela demontre que la validation est faite avant la création de l'algo.
     * 
     *  choix valeurs simulées:
     * - Arêtes défavorisées: 100, 101, 102, 3 arêtes arbitraires  et distinctes pour
     *   montrer que la liste est non vide
     * - Noeuds source/cible: 50, 60, valeurs choisies au hasard representant une requête normale
     *
     */
    @Test
    public void testRejectsUnfavouredEdgesInCH() {
        // creer le calculateur avec la fabrique simulée
        calculator = new CHPathCalculator(algoFactoryMock, algoOptions);

        // definir restrictions d'aretes défavorisées
        EdgeRestrictions restrictions = new EdgeRestrictions();
        IntArrayList unfavoredEdges = new IntArrayList();
        unfavoredEdges.add(100);
        unfavoredEdges.add(101);
        unfavoredEdges.add(102);
        restrictions.getUnfavoredEdges().addAll(unfavoredEdges);

        // Exception attendue car les arêtes défavorisées ne sont pas supportées en CH
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calcPaths(50, 60, restrictions)
        );

        // vérifier que le message d'exception mentionne les arêtes défavorisées
        assertTrue(exception.getMessage().contains("unfavored"));
        assertTrue(exception.getMessage().toLowerCase().contains("not supported"));

        // vérifier que la fabrique n'a jamais été appelée
        verify(algoFactoryMock, never()).createAlgo(any());
    }

    /**
     * Test détection d'une liste de chemins vide (cas d'erreur)
     *
     *
     * classes testees :
     * - CHPathCalculator: verifie qu'une liste de chemin vide (resultat invalide) est refusée au lieu d'être propagée.
     *
     * classes simulees:
     * - CHRoutingAlgorithmFactory: Simule fabrique algo
     * - EdgeToEdgeRoutingAlgorithm: Simule un algo bugué qui renvoi une liste vide
     *   plutot que de lever une exception ou retourner un chemin
     *
     * definition des mocks:
     * - algoFactoryMock: configure pour renvoyer algoMock quand createAlgo est appelé, controle complet de l'algo utilise.
     * - algoMock: simule un algo bugué qui retourne une liste vide de chemins.
     * 
     * choix valeurs simulées:
     * - Noeuds sources/cibles: 30, 40, noeuds qui ont des Valeurs chosies au hasard  mais distinctes, designant une requete normale.
     * - Retour de calcPaths: Collections.emptyList(), comportement invalide qu'on teste explicitement pour verifier la detection d'un invariant violé.
     */
    @Test 
    public void testDetectionEmptyPathList() {

        // Configuration de la fabrique afin de retourner un algo simulé
        when(algoFactoryMock.createAlgo(algoOptions)).thenReturn(algoMock);

        // simuler un algo bugué qui retourne une liste vide
        when(algoMock.calcPaths(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        // creer le calculateur avec la fabrique simulée
        calculator = new CHPathCalculator(algoFactoryMock, algoOptions);
        EdgeRestrictions restrictions = new EdgeRestrictions();

        // Exception attendue car l'algo retourne une liste vide
        assertThrows(
                IllegalStateException.class,
                () -> calculator.calcPaths(30, 40, restrictions)
        );
    }


}
