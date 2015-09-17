define(function(require) {

  var _ = require('underscore'),
      Router = require('react-router'),
      React = require('react'),
      showModel = require('../mixins/show-model');

  var ShowPackageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [showModel],
    whereClause: function() {
      return {packageId: this.context.router.getCurrentParams().name + "/" + this.context.router.getCurrentParams().version};
    },
    showView: function() {
      var rows = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <tr>
            <td>
              {key}
            </td>
            <td>
              {value}
            </td>
          </tr>
        );
      });
      return (
        <div>
          <h1>
            Package Details
          </h1>
          <p>
            {this.state.Model.get('description')}
          </p>
          <div className="row">
            <div className="col-md-12">
              <Router.Link to='new-campaign' params={{name: this.state.Model.get('id').name, version: this.state.Model.get('id').version}}>
                <button className="btn btn-primary pull-right">
                  NEW CAMPAIGN
                </button>
              </Router.Link>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              <table className="table table-striped table-bordered">
                <thead>
                  <tr>
                    <td>
                      {this.state.Model.get('name')}
                    </td>
                    <td>
                    </td>
                  </tr>
                </thead>
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});
