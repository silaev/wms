rs.initiate({
    "_id": "docker-rs",
    "members": [
        {"_id": 0, "host": "mongo1:50001"},
        {"_id": 1, "host": "mongo2:50002"},
        {"_id": 2, "host": "mongo3:50003"}
    ]
});