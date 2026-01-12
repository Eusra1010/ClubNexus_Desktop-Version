package org.example;

public class Event {

    private String eventId;
    private String eventName;
    private String clubName;
    private String eventDate;
    private String venue;
    private double fees;
    private String registrationDeadline;
    private boolean registrationOpen;

    public Event(String eventId, String eventName, String clubName,
                 String eventDate, String venue,
                 double fees, String registrationDeadline,
                 boolean registrationOpen) {

        this.eventId = eventId;
        this.eventName = eventName;
        this.clubName = clubName;
        this.eventDate = eventDate;
        this.venue = venue;
        this.fees = fees;
        this.registrationDeadline = registrationDeadline;
        this.registrationOpen = registrationOpen;
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getClubName() { return clubName; }
    public String getEventDate() { return eventDate; }
    public String getVenue() { return venue; }
    public double getFees() { return fees; }
    public String getRegistrationDeadline() { return registrationDeadline; }
    public boolean isRegistrationOpen() { return registrationOpen; }
}
