import React, { Component } from 'react';

export default class RequestSearch extends Component {
  static propTypes = {
    onSearch: React.PropTypes.func.isRequired,
  }

  state = {
    text: ''
  }

  handleChange = (evt) => {
    this.setState({text: evt.target.value});
  }

  handleSubmit = (evt) => {
    const text = evt.target.value.trim();
    if (evt.which === 13) {
      this.props.onSearch(text);
      this.setState({text: ''});
    }
  }

  render() {
    return (
      <div className="col-md-12">
        <h4>Get Request Details</h4>
        <input
          type="search"
          className="form-control"
          required={true}
          value={this.state.text}
          placeholder="Enter a RequestId"
          onChange={this.handleChange}
          onKeyDown={this.handleSubmit}
        />
      </div>
    );
  }
}
