import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import fuzzy from 'fuzzy';

import Utils from '../../utils';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const tableContent = (groups, filter) => {
  if (filter === '') {
    return groups;
  } else {
    const fuzzyObjects = fuzzy.filter(filter, groups, {
      extract: (group) => group.name,
      returnMatchInfo: true
    });

    return Utils.fuzzyFilter(filter, fuzzyObjects, (group) => group.name);
  }
};

const GroupsTable = ({groups, filter}) => {
  return (
    <UITable
      data={tableContent(groups, filter)}
      keyGetter={(group) => group.name}
      paginated={true}
      rowChunkSize={15}
    >
      <Column
        label="Name"
        id="name"
        key="name"
        cellData={
          (group) => (<Link to={`groups/${group.name}`} title={`Details for ${group.name}`}>{group.name}</Link>)
        }
        sortable={true}
      />
      <Column
        label="Traffic Sources"
        id="trafficSources"
        key="trafficSources"
        cellData={
          (rowData) => Utils.maybe(rowData, ['trafficSources'], []).length
        }
        sortable={true}
      />
      <Column
        label="Default Domain"
        id="domain"
        key="domain"
        cellData={
          (group) => group.defaultDomain
        }
        sortable={true}
      />
      <Column
        label=""
        id="actions"
        key="actions"
        className="actions-column"
        cellData={
          (group) => {
            return (
              <JSONButton className="inline" object={group} showOverlay={true}>
                {'{ }'}
              </JSONButton>
            );
          }
        }
      />
    </UITable>
  );
};

GroupsTable.propTypes = {
  groups: PropTypes.arrayOf(PropTypes.object),
  filter: PropTypes.string,
};

export default GroupsTable;
