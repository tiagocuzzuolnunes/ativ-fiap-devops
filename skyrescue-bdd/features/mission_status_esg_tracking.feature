# language: en
@social @mission @tracking
Feature: Mission status for real-time rescue tracking
  As a crisis response lead
  I want mission status to update in real time through the API
  So that command centers can track rescue progress and prioritize survivors (Social impact)

  Background:
    Given a rescue mission exists without an assigned drone

  Scenario: Mission status updates reflect operational progress
    When I update the mission status to "IN_PROGRESS"
    Then the mission status returned is "IN_PROGRESS"
    When I update the mission status to "COMPLETED"
    Then the mission status returned is "COMPLETED"
