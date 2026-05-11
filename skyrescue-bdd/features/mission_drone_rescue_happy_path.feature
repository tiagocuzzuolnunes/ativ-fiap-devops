# language: en
@social @mission @drone
Feature: Rescue mission with drone assignment
  As an operations coordinator
  I want to register a drone and attach it to a new rescue mission
  So that field assets are aligned with life-saving operations (Social responsibility)

  Scenario: Create a rescue mission and assign a drone
    Given a registered drone is available for rescue operations
    When I create a rescue mission linked to that drone for disaster type "FLOOD"
    Then the mission is created with HTTP status 201
    And the mission has status "PLANNED"
    And the mission lists the assigned drone id
