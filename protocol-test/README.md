## Session factory usage

<pre>

sessionFactory.query(em -> {
            var query = em.createQuery("SELECT e FROM LoggingTable e ORDER BY e.id ASC");
            query.setFirstResult((int) start);
            query.setMaxResults((int) (pageSize * 2));
            List<LoggingTable> rs = query.getResultList();
            for (var srs : rs) {
                var newItem = new FileLogListItem();
                newItem.setId(srs.getId());
                newItem.setHost(srs.getHost());
                newItem.setPath(srs.getPath());
                newItem.setTimestamp(srs.getTimestamp().getTime());
                var date = new Date(newItem.getTimestamp());
                newItem.setTime(sdf.format(date));
                result.add(newItem);

            }
        })

</pre>