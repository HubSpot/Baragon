import sys
import json
import time
import unittest
import requests
import argparse
from test import test_support

BASE_URI = '192.168.33.20:8080/baragon/v2'
AUTH_KEY = ''
MASTER_AUTH_KEY = ''
LOAD_BALANCER_GROUP = 'vagrant'
UPSTREAM = 'example.com:80'

def build_json(requestId, serviceId, basePath, addUpstream, removeUpstream, replace=None, options={}, template=None):
    data = {
        'loadBalancerRequestId': requestId,
        'loadBalancerService': {
            'serviceId': serviceId,
            'owners': ['someuser@example.com'],
            'serviceBasePath': basePath,
            'loadBalancerGroups': [LOAD_BALANCER_GROUP],
            'options': options,
            'templateName': template
        }
    }
    if addUpstream:
        data['addUpstreams'] = [{
            'upstream': addUpstream,
            'requestId': requestId
        }]
    else:
        data['addUpstreams'] = []
    if type(removeUpstream) == list:
        data['removeUpstreams'] = removeUpstream
    elif removeUpstream:
        data['removeUpstreams'] = [{
            'upstream': removeUpstream,
            'requestId': requestId
        }]
    else:
        data['removeUpstreams'] = []
    if replace:
        data['replaceServiceId'] = replace

    return json.dumps(data)

def get_request_response(requestId):
    uri = '{0}/request/{1}'.format(BASE_URI, requestId)
    params = {'authkey': AUTH_KEY}
    try:
        response = requests.get(uri, params=params)
        while response.json()['loadBalancerState'] == 'WAITING':
            time.sleep(2)
            response = requests.get(uri, params=params)
        return response.json()
    except:
        return None

def undo_request(serviceId, rename=False):
    try:
        service = get_service(serviceId)
        service = service.json() if service.status_code != 404 else None
        if service:
            params = {'authkey': AUTH_KEY}
            headers = {'Content-type': 'application/json'}
            uri = '{0}/request'.format(BASE_URI)
            json_data = build_json(serviceId + '-revert', serviceId, service['service']['serviceBasePath'], [], service['upstreams'])
            post_response = requests.post(uri,data=json_data, params=params, headers=headers)
            post_response.raise_for_status()
            return get_request_response(serviceId + '-revert')
    except:
        if not rename:
            print "Couldn't revert request for {0}, clean up might have to be done manually".format(serviceId)

def remove_service(serviceId, renamed=False):
    uri = '{0}/state/{1}'.format(BASE_URI, serviceId)
    params = {'authkey': AUTH_KEY}
    try:
        response = requests.delete(uri, params=params)
        response.raise_for_status()
    except:
        if not renamed:
            print "Couldn't remove service {0}, clean up might have to be done manually".format(serviceId)

def get_service(serviceId):
    uri = '{0}/state/{1}'.format(BASE_URI, serviceId)
    params = {'authkey': AUTH_KEY}
    try:
        response = requests.get(uri, params=params)
        return response
    except:
        return None

class Service(unittest.TestCase):
    def setUp(self):
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}

    def test_status(self):
        uri = '{0}/status'.format(BASE_URI)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json().has_key('leader'))

    def test_state(self):
        uri = '{0}/state'.format(BASE_URI)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()[0].has_key('service'))

    def test_get_service(self):
        uri = '{0}/state'.format(BASE_URI)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()[0].has_key('service'))
        serviceId = response.json()[0]['service']['serviceId']
        uri = '{0}/state/{1}'.format(BASE_URI, serviceId)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json().has_key('service'))

    def test_workers(self):
        uri = '{0}/workers'.format(BASE_URI)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(type(response.json()), list)

    def test_auth(self):
        if MASTER_AUTH_KEY and AUTH_KEY:
            uri = '{0}/auth/keys'.format(BASE_URI)
            response = requests.get(uri, params={'authkey': MASTER_AUTH_KEY})
            self.assertEqual(response.status_code, 200)
            self.assertTrue(response.json()[0].has_key('value'))

    def test_load_balancers(self):
        uri = '{0}/load-balancer'.format(BASE_URI)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(type(response.json()), list)

    def test_cluster_agents(self):
        uri = '{0}/load-balancer/{1}/agents'.format(BASE_URI, LOAD_BALANCER_GROUP)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()[0].has_key('baseAgentUri'))

    def test_cluster_known_agents(self):
        uri = '{0}/load-balancer/{1}/known-agents'.format(BASE_URI, LOAD_BALANCER_GROUP)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()[0].has_key('baseAgentUri'))

    def test_cluster_base_paths(self):
        uri = '{0}/load-balancer/{1}/base-path/all'.format(BASE_URI, LOAD_BALANCER_GROUP)
        response = requests.get(uri, params=self.params)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(type(response.json()), list)
        basePath = response.json()[0]
        uri = '{0}/load-balancer/{1}/base-path'.format(BASE_URI, LOAD_BALANCER_GROUP)
        response = requests.get(uri, params={'authkey': AUTH_KEY, 'basePath': basePath})
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json().has_key('serviceId'))

# TODO - test deletes
#    def test_delete_base_path(self):
#    def test_delete_known_agent(self):
#    def test_delete_auth_key(self):

class ValidRequest(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_valid_request(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            return True
        else:
            raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId)
        remove_service(self.randomId)

class ValidBasePathChange(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_valid_request(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            newId = self.randomId + '-2'
            sys.stderr.write('Trying to post {0}... '.format(self.randomId + '-2'))
            json_data = build_json(newId, self.randomId, '/{0}'.format(newId), UPSTREAM, None, None, {})
            post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
            self.assertEqual(post_response.status_code, 200)
            result = get_request_response(newId)
            if result['loadBalancerState'] == 'SUCCESS':
                uri = '{0}/load-balancer/test/base-path/all'.format(BASE_URI)
                basePaths = requests.get(uri, params=self.params).json()
                if '/{0}'.format(newId) not in basePaths:
                    return True
            else:
                raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId)
        remove_service(self.randomId)

class ValidRequestInvalidReplaceId(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_valid_request_invalid_replace_id(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, 'someotherservice', {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            return True
        else:
            raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId)
        remove_service(self.randomId)

class BasePathConflict(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_base_path_conflict_no_replace_id(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            newId = self.randomId + '-conflict'
            sys.stderr.write('Attempting request with basePath conflict... ')
            conflict_json_data = build_json(newId, newId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
            response = requests.post(self.uri,data=conflict_json_data, params=self.params, headers=self.headers)
            self.assertEqual(response.status_code, 200)
            result = get_request_response(newId)
            self.assertEqual(result['loadBalancerState'], 'FAILED')
        else:
            raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId, True)
        remove_service(self.randomId, True)

class BasePathConflictInvalidReplaceId(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_base_path_conflict_invalid_replace_id(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            newId = self.randomId + '-conflict'
            sys.stderr.write('Attempting request with basePath conflict... ')
            conflict_json_data = build_json(newId, newId, '/{0}'.format(self.randomId), UPSTREAM, None, 'someotherservice', {})
            response = requests.post(self.uri,data=conflict_json_data, params=self.params, headers=self.headers)
            self.assertEqual(response.status_code, 200)
            result = get_request_response(newId)
            self.assertEqual(result['loadBalancerState'], 'FAILED')
        else:
            raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId)
        remove_service(self.randomId)
        undo_request(self.randomId + '-conflict', True)
        remove_service(self.randomId, True)


class DeleteServiceId(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_delete_service(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), [], None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            sys.stderr.write('deleting service... ')
            uri = '{0}/state/{1}'.format(BASE_URI, self.randomId)
            response = requests.delete(uri, params=self.params)
            result = get_request_response(response.json()['requestId'])
            self.assertEqual(result['loadBalancerState'], 'SUCCESS')
            state_response = requests.get(uri)
            self.assertEqual(state_response.status_code, 404)

class ValidBasePathRename(unittest.TestCase):
    def setUp(self):
        timestamp = str(time.time()).replace('.','')
        self.randomId = 'Request' + timestamp
        self.renameId = 'Request' + timestamp + '-rename'
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_base_path_change(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            sys.stderr.write('trying to rename to {0}'.format(self.renameId))
            json_rename_data = build_json(self.renameId, self.renameId, '/{0}'.format(self.randomId), UPSTREAM, None, self.randomId, {})
            post_rename_response = requests.post(self.uri,data=json_rename_data, params=self.params, headers=self.headers)
            self.assertEqual(post_rename_response.status_code, 200)
            rename_result = get_request_response(self.renameId)
            if rename_result['loadBalancerState'] == 'SUCCESS':
                service_response = get_service(self.renameId)
                if service_response and service_response.status_code == 200:
                    self.assertEqual('/{0}'.format(self.randomId), service_response.json()['service']['serviceBasePath'])
                    return True
        raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up...')
        undo_request(self.randomId, True)
        undo_request(self.renameId)
        remove_service(self.randomId, True)
        remove_service(self.renameId)

class ValidBasePathServiceIdChange(unittest.TestCase):
    def setUp(self):
        timestamp = str(time.time()).replace('.','')
        self.randomId = 'Request' + timestamp
        self.renameId = 'Request' + timestamp + '-rename'
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_base_path_change(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            sys.stderr.write('trying to rename to {0}'.format(self.renameId))
            json_rename_data = build_json(self.renameId, self.renameId, '/{0}'.format(self.renameId), UPSTREAM, None, self.randomId, {})
            post_rename_response = requests.post(self.uri,data=json_rename_data, params=self.params, headers=self.headers)
            self.assertEqual(post_rename_response.status_code, 200)
            rename_result = get_request_response(self.renameId)
            if rename_result['loadBalancerState'] == 'SUCCESS':
                service_response = get_service(self.renameId)
                if service_response and service_response.status_code == 200:
                    self.assertEqual('/{0}'.format(self.renameId), service_response.json()['service']['serviceBasePath'])
                    self.assertEqual(get_service(self.randomId).status_code, 404)
                    return True
        raise Exception('Request failed, result was: {0}'.format(json_data))

    def tearDown(self):
        sys.stderr.write('Cleaning up...')
        remove_service(self.randomId, True)
        remove_service(self.renameId)

class InvalidRequest(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.randomIdReplace = 'Request' + str(time.time()).replace('.','') + '-invalidreplace'
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_invalid_request(self):
        sys.stderr.write('Posting {0} and waiting for revert... '.format(self.randomId))
        options = {'nginxExtraConfigs': ['rewrite /this_is_invalid_yo']}
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, options)
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        self.assertEqual(result['loadBalancerState'], 'FAILED')
        self.assertEqual(result['agentResponses']['REVERT'][0]['statusCode'], 200)

    def test_invalid_request_invalid_replace_id(self):
        sys.stderr.write('Posting {0} and waiting for revert... '.format(self.randomId))
        options = {'nginxExtraConfigs': ['rewrite /this_is_invalid_yo']}
        json_data = build_json(self.randomIdReplace, self.randomIdReplace, '/{0}'.format(self.randomIdReplace), UPSTREAM, None, 'someotherservice', options)
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomIdReplace)
        self.assertEqual(result['loadBalancerState'], 'FAILED')
        self.assertEqual(result['agentResponses']['REVERT'][0]['statusCode'], 200)


    def tearDown(self):
        sys.stderr.write('Cleaning up... ')
        undo_request(self.randomId, True)
        remove_service(self.randomId, True)

class InvalidRequestWithBasePathChange(unittest.TestCase):
    def setUp(self):
        timestamp = str(time.time()).replace('.','')
        self.randomId = 'Request' + timestamp
        self.renameId = 'Request' + timestamp + '-rename'
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_invalid_request_base_path_change(self):
        sys.stderr.write('Trying to post {0}... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, {})
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        if result['loadBalancerState'] == 'SUCCESS':
            sys.stderr.write('trying to rename to {0}...'.format(self.renameId))
            options = {'nginxExtraConfigs': ['rewrite /this_is_invalid_yo']}
            json_rename_data = build_json(self.renameId, self.renameId, '/{0}'.format(self.randomId), UPSTREAM, None, self.randomId, options)
            post_rename_response = requests.post(self.uri,data=json_rename_data, params=self.params, headers=self.headers)
            self.assertEqual(post_rename_response.status_code, 200)
            rename_result = get_request_response(self.renameId)
            self.assertEqual(rename_result['loadBalancerState'], 'FAILED')
            self.assertEqual(rename_result['agentResponses']['REVERT'][0]['statusCode'], 200)
            self.assertEqual(get_service(self.randomId).json()['service']['serviceBasePath'], '/{0}'.format(self.randomId))

    def tearDown(self):
        sys.stderr.write('Cleaning up...')
        undo_request(self.randomId)
        undo_request(self.renameId, True)
        remove_service(self.randomId)
        remove_service(self.renameId, True)

class InvalidTemplateName(unittest.TestCase):
    def setUp(self):
        self.randomId = 'Request' + str(time.time()).replace('.','')
        self.params = {'authkey': AUTH_KEY}
        self.headers = {'Content-type': 'application/json'}
        self.uri = '{0}/request'.format(BASE_URI)

    def test_invalid_template_name(self):
        sys.stderr.write('Posting {0} and waiting for revert... '.format(self.randomId))
        json_data = build_json(self.randomId, self.randomId, '/{0}'.format(self.randomId), UPSTREAM, None, None, None, 'invalidtemplatename')
        post_response = requests.post(self.uri,data=json_data, params=self.params, headers=self.headers)
        self.assertEqual(post_response.status_code, 200)
        result = get_request_response(self.randomId)
        self.assertEqual(result['loadBalancerState'], 'INVALID_REQUEST_NOOP')

    def tearDown(self):
        sys.stderr.write('Cleaning up...')
        undo_request(self.randomId, True)
        remove_service(self.randomId, True)

def test_main(args):
    global BASE_URI
    global AUTH_KEY
    global MASTER_AUTH_KEY
    global LOAD_BALANCER_GROUP
    global UPSTREAM
    BASE_URI = args.uri
    AUTH_KEY = args.key
    MASTER_AUTH_KEY = args.master
    LOAD_BALANCER_GROUP = args.lbGroup
    UPSTREAM = args.upstream

    if args.service:
        test_support.run_unittest(Service)
    else:
        test_support.run_unittest(
            Service,
            DeleteServiceId,
            ValidRequest,
            ValidRequestInvalidReplaceId,
            ValidBasePathServiceIdChange,
            ValidBasePathChange,
            BasePathConflict,
            BasePathConflictInvalidReplaceId,
            ValidBasePathRename,
            InvalidRequest,
            InvalidRequestWithBasePathChange,
            InvalidTemplateName
        )

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Baragon testing input')
    parser.add_argument('-u', '--uri', dest='uri', help='BaragonService base uri', default='192.168.33.20:8080/baragon/v2')
    parser.add_argument('-k', '--key', dest='key', help='BaragonService auth key')
    parser.add_argument('-m', '--master', dest='master', help='BaragonService master key')
    parser.add_argument('--upstream', dest='upstream', help='Default upstream to use in requests', default='example.com:80')
    parser.add_argument('-l', '--lbGroup', dest='lbGroup', help='Load balancer group to test with', default='vagrant')
    parser.add_argument('-s', '--service', dest='service', help='only run tests on BaragonService (no tests involving agent responses)', action='store_true')
    args = parser.parse_args()
    test_main(args)

