// explicit polyfills for older browsers
import 'core-js/es6';

import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import Utils from './utils';
import parseurl from 'parseurl';
import { useRouterHistory } from 'react-router';
import { createHistory } from 'history';

// Set up third party configurations
import { loadThirdParty } from 'thirdPartyConfigurations';

import './assets/static/images/favicon.ico';

import './styles/index.scss';
import './styles/index.styl';

function setApiRoot(data) {
  if (data.apiRoot) {
    window.localStorage.setItem('baragon.apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

const HMRContainer = (module.hot) ? require('react-hot-loader').AppContainer : ({ children }) => (children);

document.addEventListener('DOMContentLoaded', () => {
  loadThirdParty();

  if (window.config.apiRoot) {
    // set up Redux store
    const parsedUrl = parseurl({ url: config.appRoot });
    const history = useRouterHistory(createHistory)({
      basename: parsedUrl.path
    });

    const store = configureStore({}, history);

    window.app = {};

    // set up hot module reloading
    if (module.hot) {
      module.hot.accept('./router', () => {
        const NextAppRouter = require('./router').default;
        return ReactDOM.render(<HMRContainer><NextAppRouter history={history} store={store} /></HMRContainer>, document.getElementById('root'));
      });
    }

    // Render the page content
    return ReactDOM.render(<HMRContainer><AppRouter history={history} store={store} /></HMRContainer>, document.getElementById('root'), () => {
      // hide loading animation
      document.getElementById('static-loader').remove();
    });
  }

  return ReactDOM.render(
    <FormModal
      name="Set API Root"
      action="Set API Root"
      onConfirm={(data) => setApiRoot(data)}
      buttonStyle="primary"
      mustFill={true}
      formElements={[
        {
          name: 'apiRoot',
          type: FormModal.INPUT_TYPES.STRING,
          label: 'API Root URL',
          isRequired: true
        }
      ]}>
      <div id="api-prompt-message">
        <p>
          Hi there! I see you are running the Baragon UI locally.
          You must be trying to use a <strong>remote API</strong>.
        </p>
        <p>
          You need to specify an <strong>API root</strong> so BaragonUI knows where to get its data,
          e.g. <code>http://example/baragon/api</code>.
        </p>
        <p>
          This can be changed at any time in the JS console with <br />
          <code>localStorage.setItem("baragon.apiRootOverride", "http://example/baragon/api")</code>
        </p>
      </div>
    </FormModal>, document.getElementById('root')
  ).show();
});
