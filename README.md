### Usage

1. Clone the project
2. Make sure port `8080` is not in use on your local machine
3. Go to project directory on your local and run `docker compose up`
4. In order to run the client, you can go to the container terminal and run `./build/install/scoring-service/bin/client`
5. For quick testing, I would recommend installing an RPC client like BloomRPC

### Documentation of the work done

- Docker was chosen as a container solution, tried to make setup as clean and smooth as possible.
- Stack consists of Java, Gradle and gRPC.
- Scoring algorithm of a single rating is: weight * rating * constant (constant purpose is to convert output to a
  percentage).
- Aggregated score: sum of all score list based on the equation above / size of the score list.
- Heavy use of Java streams was made in order to group rating by date or ticket id.

### Improvements

- Unit and integration tests
- Input validation

# Software Engineer Test Task

As a test task for [Klaus](https://www.klausapp.com) software engineering position we ask our candidates to build a
small [gRPC](https://grpc.io) service using language of their choice. Prefered language for new services in Klaus
is [Go](https://golang.org).

The service should be using provided sample data from SQLite database (`database.db`).

Please fork this repository and share the link to your solution with us.

### Tasks

1. Come up with ticket score algorithm that accounts for rating category weights (available in `rating_categories` table). Ratings are given in a scale of 0 to 5. Score should be representable in percentages from 0 to 100.

2. Build a service that can be queried using [gRPC](https://grpc.io/docs/tutorials/basic/go/) calls and can answer following questions:

    * **Aggregated category scores over a period of time**

      E.g. what have the daily ticket scores been for a past week or what were the scores between 1st and 31st of January.

      For periods longer than one month weekly aggregates should be returned instead of daily values.

      From the reponse the following UI representation should be possible:

      | Category | Ratings | Date 1 | Date 2 | ... | Score |
        |----|----|----|----|----|----|
      | Tone | 1 | 30% | N/A | N/A | X% |
      | Grammar | 2 | N/A | 90% | 100% | X% |
      | Random | 6 | 12% | 10% | 10% | X% |

    * **Scores by ticket**

      Aggregate scores for categories within defined period by ticket.

      E.g. what aggregate category scores tickets have within defined rating time range have.

      | Ticket ID | Category 1 | Category 2 |
        |----|----|----|
      | 1   |  100%  |  30%  |
      | 2   |  30%  |  80%  |

    * **Overal quality score**

      What is the overall aggregate score for a period.

      E.g. the overall score over past week has been 96%.

    * **Period over Period score change**

      What has been the change from selected period over previous period.

      E.g. current week vs. previous week or December vs. January change in percentages.

### Bonus

* How would you build and deploy the solution?

  At Klaus we make heavy use of containers and [Kubernetes](https://kubernetes.io).