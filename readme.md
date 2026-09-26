# KORAS — Cahier des charges produit et fonctionnel

**Version : 1.0**  
**Date : septembre 2026**  
**Type : plateforme d’agent IA personnel et d’exécution**  
**Positionnement : agent numérique orienté intention, contexte et action**

---

# 1. Vision

KORAS est une plateforme d’agent IA personnel capable de transformer une intention exprimée naturellement en une suite d’actions numériques contrôlées, vérifiables et traçables.

Le modèle classique d’un assistant est :

> Question → réponse

Le modèle KORAS est :

> Intention → compréhension → planification → autorisation → exécution → vérification → résultat → mémoire

L’utilisateur ne doit pas avoir besoin de connaître la procédure technique nécessaire pour obtenir un résultat.

Exemple :

> « Trouve-moi un ordinateur portable à moins de 350 000 FCFA, compare les options disponibles et préviens-moi avant tout achat. »

KORAS doit pouvoir :

1. comprendre l’objectif ;
2. extraire les contraintes ;
3. rechercher ;
4. comparer ;
5. vérifier les informations ;
6. construire un plan ;
7. présenter les étapes importantes ;
8. demander une validation lorsqu’une action le nécessite ;
9. exécuter l’action autorisée ;
10. vérifier le résultat ;
11. conserver le contexte utile.

---

# 2. Positionnement produit

KORAS n’est pas simplement :

- un chatbot ;
- un moteur de recherche ;
- un assistant vocal ;
- un générateur de texte ;
- une application de productivité ;
- un navigateur automatisé.

KORAS est :

> **une plateforme d’agents capables de comprendre une intention humaine et d’exécuter des tâches numériques sous contrôle de l’utilisateur.**

La valeur centrale repose sur :

- compréhension ;
- contexte ;
- mémoire ;
- outils ;
- automatisation ;
- permissions ;
- exécution ;
- vérification.

---

# 3. Principes produit

## 3.1 Intention avant procédure

L’utilisateur décrit le résultat recherché.

KORAS détermine les étapes nécessaires.

## 3.2 Action avant génération

Une réponse textuelle n’est pas considérée comme une tâche accomplie.

Une tâche est accomplie lorsque le résultat attendu est obtenu ou que KORAS explique précisément pourquoi il n’a pas pu l’obtenir.

## 3.3 Contrôle humain

KORAS peut agir de manière autonome dans les limites autorisées, mais les opérations sensibles nécessitent une validation explicite.

## 3.4 Transparence

L’utilisateur doit toujours pouvoir savoir :

- ce que KORAS fait ;
- pourquoi il le fait ;
- quelle information il utilise ;
- quelle action il s’apprête à effectuer ;
- ce qui a réussi ;
- ce qui a échoué.

## 3.5 Mémoire contrôlable

La mémoire appartient à l’utilisateur.

L’utilisateur doit pouvoir :

- consulter ;
- corriger ;
- supprimer ;
- désactiver ;
- exporter.

---

# 4. Utilisateurs cibles

## 4.1 Utilisateur individuel

- étudiants ;
- professionnels ;
- entrepreneurs ;
- indépendants ;
- chercheurs ;
- créateurs ;
- voyageurs ;
- particuliers.

## 4.2 Utilisateur avancé

Utilisateur souhaitant déléguer :

- recherche ;
- organisation ;
- achats ;
- administration ;
- communication ;
- planification ;
- suivi de projets.

## 4.3 Entreprises

- PME ;
- startups ;
- équipes commerciales ;
- équipes administratives ;
- dirigeants ;
- consultants.

---

# 5. Applications

L’écosystème KORAS est constitué de plusieurs surfaces.

```text
KORAS
|
|-- Application mobile
|-- Application web
|-- Application desktop
|-- Agent navigateur
|-- Console d’administration
`-- Developer Platform
```

La première version doit privilégier une application mobile et une interface web avant de multiplier les clients.

---

# 6. Application mobile

## 6.1 Objectif

L’application mobile est l’interface principale entre l’utilisateur et son agent.

Elle doit permettre en temps reel via la technologie accessibiliter d android :

- conversation ;
- voix ;
- image ;
- documents ;
- suivi des tâches ;
- validation des actions ;
- mémoire ;
- notifications ;
- historique ;
- paramètres.

## 6.2 Écrans

```text
Splash
Onboarding
Authentification