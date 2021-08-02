
fun <T> getUntilNotNull(intervalMillisecond: Long, timeoutsMillisecond: Long, getFunc: () -> T?): T? {
    val start = System.currentTimeMillis()
    var m = getFunc()
    while(m == null && System.currentTimeMillis() - start < timeoutsMillisecond){
        sleep(intervalMillisecond)
        m = getFunc()
    }
    return  m
}
