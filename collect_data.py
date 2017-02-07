from __future__ import division

import os
import re
import time
import webhose

# Initializing webhose SDK with our private TOKEN
API_TOKEN = 'YOUR_WEBHOSE_API_TOKEN'
webhose.config(API_TOKEN)

# Setting the relative location of the train/test files
resources_dir = './src/main/resources'


def collect(filename, query, limit, sentiment, partition):
    lines = set()

    # Collect the data from webhose.io with the given query up to the given limit
    r = webhose.search(query)
    while len(r.posts) > 0 and len(lines) < limit:
        for post in r.posts:
            if 1000 > len(post.text) > 50:
                text = re.sub(r'(\([^\)]+\)|(stars|rating)\s*:\s*\S+)\s*$', '', post.text.replace('\n', '').replace('\t', ''), 0, re.I)
                lines.add(text.encode('utf8'))
        time.sleep(2)
        print 'Getting %s' % r.next
        r = r.get_next()

    # Build the train file (first part of the returned documents)
    with open(os.path.join(resources_dir, filename + '.train'), 'a+') as train_file:
        for line in list(lines)[:int((len(lines))*partition)]:
            train_file.write('%s\t%s\n' % (sentiment, line))

    # Build the test file (rest of the returned documents)
    with open(os.path.join(resources_dir, filename + '.test'), 'a+') as test_file:
        for line in list(lines)[int((len(lines))*partition):]:
            test_file.write('%s\t%s\n' % (sentiment, line))


if __name__ == '__main__':
    # Create the resources directory if not exists
    if not os.path.exists(resources_dir):
        os.makedirs(resources_dir)

    # Get reviews from various sources for training and testing the general classifier
    collect('general', 'language:english AND rating:>4 -site:booking.com -site:expedia.*', 400, 'positive', 4/5)
    collect('general', 'language:english AND rating:<2 -site:booking.com -site:expedia.*', 400, 'negative', 4/5)

    # Get reviews from booking.com for training and testing the domain-specific classifier
    collect('booking', 'language:english AND rating:>4 AND site:booking.com', 400, 'positive', 4/5)
    collect('booking', 'language:english AND rating:<2 AND site:booking.com', 400, 'negative', 4/5)

    # Get reviews from expedia.com for a later tests
    collect('expedia', 'language:english AND rating:>4 AND site:expedia.com', 300, 'positive', 0)
    collect('expedia', 'language:english AND rating:<2 AND site:expedia.com', 300, 'negative', 0)

