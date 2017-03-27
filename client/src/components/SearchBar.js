import React, {Component} from 'react'
import {Form, Button, InputGroup, FormControl, Popover, OverlayTrigger} from 'react-bootstrap'
import autobind from 'autobind-decorator'

import AdvancedSearch from 'components/AdvancedSearch'
import History from 'components/History'

import SEARCH_ICON from 'resources/search-alt.png'

export default class SearchBar extends Component {
	componentWillMount() {
		this.setQueryState()
		this.unregisterHistoryListener = History.listen(this.setQueryState)
	}

	componentWillUnmount() {
		this.unregisterHistoryListener()
	}

	render() {
		let advancedSearchPopover = <Popover id="advanced-search" placement="bottom" title="Advanced search">
			<AdvancedSearch />
		</Popover>

		return <Form onSubmit={this.onSubmit}>
			<InputGroup>
				<FormControl value={this.state.query} placeholder="Search for people, reports, positions, or locations" onChange={this.onChange} id="searchBarInput" />
				<InputGroup.Button>
					<Button onClick={this.onSubmit} id="searchBarSubmit"><img src={SEARCH_ICON} height={16} alt="Search" /></Button>
				</InputGroup.Button>
			</InputGroup>

			<OverlayTrigger trigger="click" placement="bottom" overlay={advancedSearchPopover}>
				<small><a>Advanced search</a></small>
			</OverlayTrigger>
		</Form>
	}

	@autobind
	setQueryState() {
		this.setState({query: History.getCurrentLocation().query.text || ''})
	}

	@autobind
	onChange(event) {
		this.setState({query: event.target.value})
	}

	@autobind
	onSubmit(event) {
		History.push({pathname: '/search', query: {text: this.state.query}})
		event.preventDefault()
		event.stopPropagation()
	}
}
