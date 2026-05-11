# language: en
@governance @victim @validation
Feature: Victim registration governance
  As a compliance officer
  I want victim detections to be rejected when the mission does not exist
  So that audit trails and data integrity are preserved (Governance)

  Scenario: Attempting to register a victim to a non-existent mission
    Given no mission exists with id 999999
    When I attempt to register a victim on mission 999999
    Then the API responds with HTTP status 404
    And the error payload indicates the mission was not found
